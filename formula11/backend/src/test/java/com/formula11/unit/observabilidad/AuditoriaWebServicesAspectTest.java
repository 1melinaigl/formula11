package com.formula11.unit.observabilidad;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.formula11.observabilidad.AuditoriaWebServicesAspect;
import com.formula11.observabilidad.EnmascaradorParametros;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditoriaWebServicesAspectTest {
    private final Logger loggerAuditoria = (Logger) LoggerFactory.getLogger(AuditoriaWebServicesAspect.LOGGER_AUDITORIA);
    private final Logger loggerAspecto = (Logger) LoggerFactory.getLogger(AuditoriaWebServicesAspect.class);
    private final ListAppender<ILoggingEvent> lineas = new ListAppender<>();
    private Level nivelAnterior;
    private final ListAppender<ILoggingEvent> avisos = new ListAppender<>();

    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    private final MethodSignature firma = mock(MethodSignature.class);

    private AuditoriaWebServicesAspect aspecto;

    static class EjemploController { }

    @BeforeEach
    void preparar() {
        lineas.start();
        avisos.start();
        loggerAuditoria.addAppender(lineas);
        // Otros tests de la misma JVM (perfil e2e) dejan el root en WARN: el test fija su nivel.
        nivelAnterior = loggerAuditoria.getLevel();
        loggerAuditoria.setLevel(Level.INFO);
        loggerAspecto.addAppender(avisos);
        aspecto = new AuditoriaWebServicesAspect(
                new EnmascaradorParametros(JsonMapper.builder().findAndAddModules().build()));

        when(joinPoint.getSignature()).thenReturn(firma);
        doReturn(EjemploController.class).when(firma).getDeclaringType();
        when(firma.getName()).thenReturn("listar");
        when(firma.getParameterNames()).thenReturn(new String[]{"page"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{0});
    }

    @AfterEach
    void limpiar() {
        loggerAuditoria.detachAppender(lineas);
        loggerAuditoria.setLevel(nivelAnterior);
        loggerAspecto.detachAppender(avisos);
        SecurityContextHolder.clearContext();
    }

    @Test
    void registraUnaLineaConElUsuarioAutenticado() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ana@mail.com", null, List.of()));
        when(joinPoint.proceed()).thenReturn("respuesta");

        Object respuesta = aspecto.auditar(joinPoint);

        assertEquals("respuesta", respuesta);
        assertEquals(1, lineas.list.size());
        ILoggingEvent evento = lineas.list.get(0);
        assertEquals(Level.INFO, evento.getLevel());
        String linea = evento.getFormattedMessage();
        assertTrue(linea.startsWith(
                "usuario=ana@mail.com operacion=EjemploController.listar parametros={page=0} duracionMs="), linea);
        assertTrue(linea.endsWith(" resultado=OK"), linea);
    }

    @Test
    void sinAutenticacionElUsuarioEsAnonimo() throws Throwable {
        when(joinPoint.proceed()).thenReturn(null);

        aspecto.auditar(joinPoint);

        assertTrue(lineas.list.get(0).getFormattedMessage().startsWith("usuario=anónimo "));
    }

    @Test
    void conAutenticacionAnonimaElUsuarioEsAnonimo() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        when(joinPoint.proceed()).thenReturn(null);

        aspecto.auditar(joinPoint);

        assertTrue(lineas.list.get(0).getFormattedMessage().startsWith("usuario=anónimo "));
    }

    @Test
    void siElControllerFallaRegistraElErrorYRelanzaLaMismaExcepcion() throws Throwable {
        IllegalStateException error = new IllegalStateException("falló");
        when(joinPoint.proceed()).thenThrow(error);

        IllegalStateException lanzada = assertThrows(IllegalStateException.class, () -> aspecto.auditar(joinPoint));

        assertSame(error, lanzada);
        assertEquals(1, lineas.list.size());
        assertTrue(lineas.list.get(0).getFormattedMessage().endsWith(" resultado=ERROR(IllegalStateException)"));
    }

    @Test
    void siFallaArmarLaLineaElEndpointRespondeIgual() throws Throwable {
        EnmascaradorParametros roto = mock(EnmascaradorParametros.class);
        when(roto.enmascarar(any(), any())).thenThrow(new IllegalStateException("roto"));
        aspecto = new AuditoriaWebServicesAspect(roto);
        when(joinPoint.proceed()).thenReturn("respuesta");

        Object respuesta = aspecto.auditar(joinPoint);

        assertEquals("respuesta", respuesta);
        assertTrue(lineas.list.isEmpty());
        assertEquals(1, avisos.list.size());
        assertEquals(Level.WARN, avisos.list.get(0).getLevel());
    }
}
