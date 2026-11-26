package com.formula11.integration.observabilidad;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.formula11.controller.cotizaciones.CotizacionJugadorController;
import com.formula11.controller.jugadores.JugadorController;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.observabilidad.AuditoriaWebServicesAspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AuditoriaWebServicesIntegrationTest {
    static {
        System.setProperty("api.version", "1.40");
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JugadorController jugadorController;
    @Autowired
    private CotizacionJugadorController cotizacionJugadorController;

    private final Logger loggerAuditoria = (Logger) LoggerFactory.getLogger(AuditoriaWebServicesAspect.LOGGER_AUDITORIA);
    private final ListAppender<ILoggingEvent> lineas = new ListAppender<>();
    private Level nivelAnterior;

    @BeforeEach
    void capturarLineas() {
        lineas.start();
        loggerAuditoria.addAppender(lineas);
        // Otros tests de la misma JVM (perfil e2e) dejan el root en WARN: el test fija su nivel.
        nivelAnterior = loggerAuditoria.getLevel();
        loggerAuditoria.setLevel(Level.INFO);
    }

    @AfterEach
    void soltarLineas() {
        loggerAuditoria.detachAppender(lineas);
        loggerAuditoria.setLevel(nivelAnterior);
    }

    @Test
    void losControllersQuedanEnvueltosPorElAspecto() {
        assertTrue(AopUtils.isAopProxy(jugadorController));
        assertTrue(AopUtils.isAopProxy(cotizacionJugadorController));
    }

    @Test
    void unaLlamadaExitosaDejaUnaLineaConResultadoOk() {
        jugadorController.listar(0, 20, null, "La Liga", null, null, Map.of());

        assertEquals(1, lineas.list.size());
        String linea = lineas.list.get(0).getFormattedMessage();
        assertTrue(linea.startsWith("usuario=anónimo operacion=JugadorController.listar parametros={page=0, size=20, "
                + "sort=null, liga=La Liga, equipo=null, posicion=null, parametros={}} duracionMs="), linea);
        assertTrue(linea.endsWith(" resultado=OK"), linea);
    }

    @Test
    void unaLlamadaConErrorDejaUnaLineaConElTipoDeError() {
        assertThrows(RecursoNoEncontradoException.class, () -> cotizacionJugadorController.detalle(999_999L));

        assertEquals(1, lineas.list.size());
        String linea = lineas.list.get(0).getFormattedMessage();
        assertTrue(linea.contains("operacion=CotizacionJugadorController.detalle parametros={jugadorId=999999}"), linea);
        assertTrue(linea.endsWith(" resultado=ERROR(RecursoNoEncontradoException)"), linea);
    }
}
