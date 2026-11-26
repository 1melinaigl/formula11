package com.formula11.observabilidad;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Deja una línea en el logger {@value #LOGGER_AUDITORIA} por cada llamada a un endpoint de la API,
 * termine bien o con error. Una falla al armar la línea nunca cambia la respuesta del endpoint.
 */
@Aspect
@Component
public class AuditoriaWebServicesAspect {
    public static final String LOGGER_AUDITORIA = "AUDITORIA";
    public static final String ANONIMO = "anónimo";

    private static final Logger AUDITORIA = LoggerFactory.getLogger(LOGGER_AUDITORIA);
    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaWebServicesAspect.class);

    private final EnmascaradorParametros enmascarador;

    public AuditoriaWebServicesAspect(EnmascaradorParametros enmascarador) {
        this.enmascarador = enmascarador;
    }

    @Around("within(com.formula11.controller..*) && @within(org.springframework.web.bind.annotation.RestController)")
    public Object auditar(ProceedingJoinPoint joinPoint) throws Throwable {
        long inicio = System.nanoTime();
        String resultado = LineaAuditoria.OK;
        try {
            return joinPoint.proceed();
        } catch (Throwable error) {
            resultado = LineaAuditoria.resultadoError(error);
            throw error;
        } finally {
            registrar(joinPoint, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio), resultado);
        }
    }

    private void registrar(ProceedingJoinPoint joinPoint, long duracionMs, String resultado) {
        String operacion = null;
        try {
            MethodSignature firma = (MethodSignature) joinPoint.getSignature();
            operacion = firma.getDeclaringType().getSimpleName() + "." + firma.getName();
            String parametros = enmascarador.enmascarar(firma.getParameterNames(), joinPoint.getArgs());
            AUDITORIA.info(new LineaAuditoria(usuarioActual(), operacion, parametros, duracionMs, resultado).formatear());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo registrar la auditoría de {}: {}", operacion, e.getMessage());
        }
    }

    private static String usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return ANONIMO;
        }
        return autenticacion.getName();
    }
}
