package com.formula11.unit.service;

import com.formula11.dto.RecalculoRequest;
import com.formula11.model.Estrategia;
import com.formula11.service.*;
import com.formula11.persistence.EstrategiaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RecalculoServiceTest {
    @Mock RecalculoSemanaService semanas;
    @Mock EstrategiaRepository estrategias;
    @Mock com.formula11.persistence.CotizacionRepository cotizaciones;
    @Mock CotizacionCacheInvalidator cache;

    @Test
    void sinCuerpoProcesaUltimoDomingoCerrado() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-07T00:00:00Z"), ZoneOffset.UTC);
        when(semanas.procesar(LocalDate.of(2026, 1, 4)))
                .thenReturn(new RecalculoSemanaService.SemanaResultado(4, 1));
        when(estrategias.findAll()).thenReturn(List.of(estrategia()));

        var result = new RecalculoService(clock, semanas, estrategias, cotizaciones, cache).recalcular(null);

        assertEquals(1, result.semanasCalculadas());
        verify(semanas).procesar(LocalDate.of(2026, 1, 4));
        verify(cache).invalidate();
    }

    @Test
    void procesaRangoInclusivoYRechazaRangoInvalido() {
        Clock clock = Clock.systemUTC();
        when(semanas.procesar(any())).thenReturn(new RecalculoSemanaService.SemanaResultado(1, 0));
        when(estrategias.findAll()).thenReturn(List.of(estrategia()));
        when(cotizaciones.findMaxFecha()).thenReturn(Optional.empty());
        var service = new RecalculoService(clock, semanas, estrategias, cotizaciones, cache);

        var response = service.recalcular(new RecalculoRequest(
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 18)));

        assertEquals(2, response.semanasCalculadas());
        verify(semanas).procesar(LocalDate.of(2026, 1, 11));
        verify(semanas).procesar(LocalDate.of(2026, 1, 18));
        assertThrows(IllegalArgumentException.class, () -> service.recalcular(
                new RecalculoRequest(LocalDate.of(2026, 1, 19), LocalDate.of(2026, 1, 18))));
        assertThrows(IllegalArgumentException.class, () -> service.recalcular(
                new RecalculoRequest(LocalDate.of(2026, 1, 19), null)));
    }

    @Test
    void extiendeElBackfillHastaLaUltimaSemanaPersistida() {
        when(semanas.procesar(any())).thenReturn(new RecalculoSemanaService.SemanaResultado(1, 0));
        when(estrategias.findAll()).thenReturn(List.of(estrategia()));
        when(cotizaciones.findMaxFecha()).thenReturn(Optional.of(LocalDate.of(2026, 1, 25)));

        var service = new RecalculoService(Clock.systemUTC(), semanas, estrategias, cotizaciones, cache);
        var response = service.recalcular(new RecalculoRequest(
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11)));

        assertEquals(3, response.semanasCalculadas());
        verify(semanas).procesar(LocalDate.of(2026, 1, 25));
    }

    @Test
    void devuelveContadoresEstrategiasYCorrelationId() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-07T00:00:00Z"), ZoneOffset.UTC);
        when(semanas.procesar(any())).thenReturn(new RecalculoSemanaService.SemanaResultado(4, 2));
        when(estrategias.findAll()).thenReturn(List.of(estrategia()));

        var response = new RecalculoService(clock, semanas, estrategias, cotizaciones, cache).recalcular(null);

        assertAll(
                () -> assertEquals("COMPLETADO", response.estado()),
                () -> assertEquals(4, response.jugadoresCotizados()),
                () -> assertEquals(2, response.jugadoresSinMetricas()),
                () -> assertNotNull(response.inicio()),
                () -> assertNotNull(response.fin()),
                () -> assertNotNull(response.correlationId()),
                () -> assertEquals(1, response.estrategias().size()));
    }

    @Test
    void rechazaUnSegundoRecalculoMientrasElPrimeroEstaEnCurso() throws Exception {
        CountDownLatch entra = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        when(semanas.procesar(any())).thenAnswer(invocation -> {
            entra.countDown();
            liberar.await(2, TimeUnit.SECONDS);
            return new RecalculoSemanaService.SemanaResultado(1, 0);
        });
        when(estrategias.findAll()).thenReturn(List.of(estrategia()));
        RecalculoService service = new RecalculoService(Clock.systemUTC(), semanas, estrategias, cotizaciones, cache);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = executor.submit(() -> service.recalcular(null));
            assertTrue(entra.await(2, TimeUnit.SECONDS));
            assertThrows(IllegalStateException.class, () -> service.recalcular(null));
            liberar.countDown();
            first.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Estrategia estrategia() {
        return new Estrategia("RENDIMIENTO_POSICION", "Rendimiento", 1,
                Map.of("DEL", Map.of("goles", 1d)),
                Map.of("k", 0.4d, "topeVariacion", 0.2d, "piso", 0.1d),
                true, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
