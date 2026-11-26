package com.formula11.unit.observabilidad;

import com.formula11.model.metricas.EjecucionMetricas;
import com.formula11.observabilidad.CargaMetricasHealthIndicator;
import com.formula11.service.metricas.CargaMetricasService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CargaMetricasHealthIndicatorTest {
    private static final Instant INICIO = Instant.parse("2026-11-23T06:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-23T06:12:40Z");

    private final CargaMetricasService service = mock(CargaMetricasService.class);
    private final CargaMetricasHealthIndicator indicador = new CargaMetricasHealthIndicator(service);

    @Test
    void sinCargasEstaArribaYLoDice() {
        when(service.ultimaTerminada()).thenReturn(Optional.empty());

        Health health = indicador.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("Sin cargas de métricas", health.getDetails().get("detalle"));
    }

    @Test
    void conLaUltimaCargaCompletadaEstaArribaConElDetalle() {
        when(service.ultimaTerminada()).thenReturn(Optional.of(terminada("COMPLETADA")));

        Health health = indicador.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("COMPLETADA", health.getDetails().get("estado"));
        assertEquals(FIN, health.getDetails().get("fin"));
        assertFalse(health.getDetails().containsKey("error"));
    }

    @Test
    void conLaUltimaCargaParcialSigueArriba() {
        when(service.ultimaTerminada()).thenReturn(Optional.of(terminada("PARCIAL")));

        Health health = indicador.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("PARCIAL", health.getDetails().get("estado"));
    }

    @Test
    void conLaUltimaCargaFallidaEstaCaidaConElError() {
        EjecucionMetricas fallida = new EjecucionMetricas("EN_CURSO", INICIO, "corr-1");
        fallida.fallar(FIN, "No se pudo descargar la liga");
        when(service.ultimaTerminada()).thenReturn(Optional.of(fallida));

        Health health = indicador.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("FALLIDA", health.getDetails().get("estado"));
        assertEquals(FIN, health.getDetails().get("fin"));
        assertEquals("No se pudo descargar la liga", health.getDetails().get("error"));
    }

    private static EjecucionMetricas terminada(String estado) {
        EjecucionMetricas ejecucion = new EjecucionMetricas("EN_CURSO", INICIO, "corr-1");
        ejecucion.completar(estado, FIN);
        return ejecucion;
    }
}
