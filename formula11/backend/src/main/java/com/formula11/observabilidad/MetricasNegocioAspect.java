package com.formula11.observabilidad;

import com.formula11.service.mercado.ResultadoOperacion;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Alimenta las métricas de negocio desde los services sin tocarlos: cuenta las compras y ventas
 * confirmadas y mide cada recálculo de cotizaciones (manual o del job semanal). Corre por fuera de
 * {@code @Transactional}, así que cuando cuenta una operación el commit ya ocurrió.
 */
@Aspect
@Component
@Order(MetricasNegocioAspect.ORDEN)
public class MetricasNegocioAspect {
    /**
     * Después del ExposeInvocationInterceptor de Spring (HIGHEST_PRECEDENCE + 1), que necesita
     * {@code @AfterReturning}, y antes que {@code @Transactional} (LOWEST_PRECEDENCE).
     */
    static final int ORDEN = Ordered.HIGHEST_PRECEDENCE + 10;

    private static final Logger log = LoggerFactory.getLogger(MetricasNegocioAspect.class);

    private final MetricasNegocio metricas;

    public MetricasNegocioAspect(MetricasNegocio metricas) {
        this.metricas = metricas;
    }

    @AfterReturning(pointcut = "execution(* com.formula11.service.mercado.MercadoService.comprar(..))"
            + " || execution(* com.formula11.service.mercado.MercadoService.vender(..))", returning = "resultado")
    public void contarOperacion(ResultadoOperacion resultado) {
        try {
            if (resultado == null || resultado.transaccion() == null) {
                return;
            }
            // Por nombre: si el modelo suma un tipo (por ejemplo, P2P), se cuenta sin tocar este aspecto.
            String tipo = resultado.transaccion().getTipo().name();
            metricas.registrarOperacionMercado(TipoOperacionMercado.valueOf(tipo));
        } catch (RuntimeException e) {
            log.warn("No se pudo contar la operación de mercado: {}", e.getMessage());
        }
    }

    @Around("execution(* com.formula11.service.cotizaciones.RecalculoService.recalcular(..))")
    public Object medirRecalculo(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample muestra = iniciar();
        boolean ok = false;
        try {
            Object resultado = joinPoint.proceed();
            ok = true;
            return resultado;
        } finally {
            cerrar(muestra, ok);
        }
    }

    /** Una falla al medir no puede tapar el resultado ni la excepción del recálculo. */
    private void cerrar(Timer.Sample muestra, boolean ok) {
        try {
            metricas.registrarRecalculo(muestra, ok);
        } catch (RuntimeException e) {
            log.warn("No se pudo medir el recálculo: {}", e.getMessage());
        }
    }

    private Timer.Sample iniciar() {
        try {
            return metricas.iniciarMedicion();
        } catch (RuntimeException e) {
            log.warn("No se pudo empezar a medir el recálculo: {}", e.getMessage());
            return null;
        }
    }
}
