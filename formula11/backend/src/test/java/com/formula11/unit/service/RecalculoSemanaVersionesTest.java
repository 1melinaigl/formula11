package com.formula11.unit.service;

import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.MetricaPartidoRepository;
import com.formula11.service.RecalculoSemanaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Después de crear una versión nueva, el recálculo usa solo la última versión de cada estrategia. */
class RecalculoSemanaVersionesTest {

    private static Estrategia rendimiento(int version) {
        return new Estrategia("RENDIMIENTO_POSICION", "Rendimiento", version,
                Map.of("DEL", Map.of("rating", 1d)),
                Map.of("k", 0.4d, "topeVariacion", 0.2d, "piso", 0.1d),
                version == 1, Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void conDosVersionesGuardaUnaSolaCotizacionPorJugadorConLaUltima() {
        JugadorRepository jugadores = mock(JugadorRepository.class);
        MetricaPartidoRepository metricas = mock(MetricaPartidoRepository.class);
        EstrategiaRepository estrategias = mock(EstrategiaRepository.class);
        CotizacionRepository cotizaciones = mock(CotizacionRepository.class);
        Jugador jugador = mock(Jugador.class);
        when(jugador.getId()).thenReturn(1L);
        when(jugadores.findAll()).thenReturn(List.of(jugador));
        when(metricas.findByFechaPartidoBetween(any(), any())).thenReturn(List.of());
        when(estrategias.findAll()).thenReturn(List.of(rendimiento(1), rendimiento(2)));
        LocalDate domingo = LocalDate.of(2026, 10, 25);
        when(cotizaciones.findByFechaLessThanOrderByFechaDesc(domingo)).thenReturn(List.of());

        new RecalculoSemanaService(jugadores, metricas, estrategias, cotizaciones).procesar(domingo);

        ArgumentCaptor<List<Cotizacion>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(cotizaciones).saveAll(guardadas.capture());
        assertEquals(1, guardadas.getValue().size());
        assertEquals(2, guardadas.getValue().get(0).getVersion());
    }
}
