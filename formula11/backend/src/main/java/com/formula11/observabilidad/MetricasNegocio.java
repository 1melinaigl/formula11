package com.formula11.observabilidad;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumMap;
import java.util.Map;

/**
 * Métricas de negocio para Prometheus: operaciones de mercado confirmadas por tipo y duración del
 * recálculo de cotizaciones. Nunca lanza excepciones: una falla al medir no cambia ninguna operación.
 */
@Component
public class MetricasNegocio {
    public static final String OPERACIONES_MERCADO = "formula11.mercado.operaciones";
    public static final String RECALCULO = "formula11.cotizaciones.recalculo";

    private static final Logger log = LoggerFactory.getLogger(MetricasNegocio.class);

    private final MeterRegistry registry;
    private final Map<TipoOperacionMercado, Counter> operaciones = new EnumMap<>(TipoOperacionMercado.class);

    public MetricasNegocio(MeterRegistry registry) {
        this.registry = registry;
        // Los tres tipos se registran al arrancar para que aparezcan en 0 aunque no haya operaciones.
        for (TipoOperacionMercado tipo : TipoOperacionMercado.values()) {
            operaciones.put(tipo, Counter.builder(OPERACIONES_MERCADO)
                    .description("Operaciones de mercado confirmadas por tipo")
                    .tag("tipo", tipo.etiqueta())
                    .register(registry));
        }
    }

    /**
     * Suma una operación confirmada. Si se llama dentro de una transacción, suma después del commit,
     * así una operación que termina en rollback no cuenta.
     */
    public void registrarOperacionMercado(TipoOperacionMercado tipo) {
        if (tipo == null) {
            log.warn("Operación de mercado sin tipo: no se cuenta");
            return;
        }
        try {
            Counter contador = operaciones.get(tipo);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        contador.increment();
                    }
                });
            } else {
                contador.increment();
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo contar la operación de mercado {}: {}", tipo, e.getMessage());
        }
    }

    /** Empieza a medir un recálculo; se cierra con {@link #registrarRecalculo}. */
    public Timer.Sample iniciarMedicion() {
        return Timer.start(registry);
    }

    /** Cierra la medición de un recálculo con su resultado ({@code ok} o {@code error}). */
    public void registrarRecalculo(Timer.Sample muestra, boolean ok) {
        if (muestra == null) {
            return;
        }
        try {
            muestra.stop(Timer.builder(RECALCULO)
                    .description("Duración del recálculo de cotizaciones")
                    .tag("resultado", ok ? "ok" : "error")
                    .register(registry));
        } catch (RuntimeException e) {
            log.warn("No se pudo medir el recálculo de cotizaciones: {}", e.getMessage());
        }
    }
}
