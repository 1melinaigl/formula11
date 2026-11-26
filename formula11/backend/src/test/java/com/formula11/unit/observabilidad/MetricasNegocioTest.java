package com.formula11.unit.observabilidad;

import com.formula11.observabilidad.MetricasNegocio;
import com.formula11.observabilidad.TipoOperacionMercado;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MetricasNegocioTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MetricasNegocio metricas = new MetricasNegocio(registry);

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void losTresTiposExistenEnCeroDesdeElArranque() {
        for (TipoOperacionMercado tipo : TipoOperacionMercado.values()) {
            assertEquals(0.0, operaciones(tipo.etiqueta()), tipo.name());
        }
        assertEquals("compra", TipoOperacionMercado.COMPRA.etiqueta());
        assertEquals("p2p", TipoOperacionMercado.P2P.etiqueta());
    }

    @Test
    void sinTransaccionSumaEnElMomento() {
        metricas.registrarOperacionMercado(TipoOperacionMercado.COMPRA);
        metricas.registrarOperacionMercado(TipoOperacionMercado.COMPRA);
        metricas.registrarOperacionMercado(TipoOperacionMercado.P2P);

        assertEquals(2.0, operaciones("compra"));
        assertEquals(0.0, operaciones("venta"));
        assertEquals(1.0, operaciones("p2p"));
    }

    @Test
    void dentroDeUnaTransaccionSumaRecienDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        metricas.registrarOperacionMercado(TipoOperacionMercado.VENTA);
        assertEquals(0.0, operaciones("venta"));

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertEquals(1.0, operaciones("venta"));
    }

    @Test
    void siLaTransaccionHaceRollbackNoSuma() {
        TransactionSynchronizationManager.initSynchronization();

        metricas.registrarOperacionMercado(TipoOperacionMercado.VENTA);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertEquals(0.0, operaciones("venta"));
    }

    @Test
    void unTipoNuloNoRompeNiSuma() {
        assertDoesNotThrow(() -> metricas.registrarOperacionMercado(null));
        assertEquals(0.0, operaciones("compra") + operaciones("venta") + operaciones("p2p"));
    }

    @Test
    void elTimerDelRecalculoSeparaPorResultado() {
        metricas.registrarRecalculo(metricas.iniciarMedicion(), true);
        metricas.registrarRecalculo(metricas.iniciarMedicion(), true);
        metricas.registrarRecalculo(metricas.iniciarMedicion(), false);

        assertEquals(2, recalculo("ok").count());
        assertEquals(1, recalculo("error").count());
    }

    @Test
    void sinMuestraNoRegistraNada() {
        assertDoesNotThrow(() -> metricas.registrarRecalculo(null, true));
        assertNull(registry.find(MetricasNegocio.RECALCULO).timer());
    }

    private double operaciones(String tipo) {
        return registry.get(MetricasNegocio.OPERACIONES_MERCADO).tag("tipo", tipo).counter().count();
    }

    private Timer recalculo(String resultado) {
        return registry.get(MetricasNegocio.RECALCULO).tag("resultado", resultado).timer();
    }
}
