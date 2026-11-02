package com.formula11.unit.service;

import com.formula11.dto.RecalculoResponse;
import com.formula11.service.ActualizacionMetricasPort;
import com.formula11.service.CotizacionScheduler;
import com.formula11.service.NoOpActualizacionMetricasAdapter;
import com.formula11.service.RecalculoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotizacionSchedulerTest {
    @Mock ActualizacionMetricasPort actualizacion;
    @Mock RecalculoService recalculo;

    @AfterEach
    void limpiaMdc() {
        MDC.clear();
    }

    @Test
    void recalculaAunqueLaActualizacionSeaExitosa() {
        ejecutar();

        verify(actualizacion).actualizar();
        verify(recalculo).recalcular(null);
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void recalculaAunqueLaActualizacionFalle() {
        doThrow(new IllegalStateException("servicio no disponible")).when(actualizacion).actualizar();

        ejecutar();

        verify(recalculo).recalcular(null);
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void recalculaConLaImplementacionInicialNoOperativa() {
        ActualizacionMetricasPort noOperativa = new NoOpActualizacionMetricasAdapter();
        new CotizacionScheduler(noOperativa, recalculo, Clock.systemUTC()).ejecutar();

        verify(recalculo).recalcular(null);
        assertNull(MDC.get("correlationId"));
    }

    private void ejecutar() {
        new CotizacionScheduler(actualizacion, recalculo,
                Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneOffset.UTC)).ejecutar();
    }
}
