package com.formula11.unit.service;

import com.formula11.adapter.whoscored.PartidoWhoScoredResumen;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.MetricaPartido;
import com.formula11.model.PartidoExterno;
import com.formula11.model.PendienteVinculacion;
import com.formula11.model.whoscored.MatchCentreParser;
import com.formula11.persistence.MetricaPartidoRepository;
import com.formula11.persistence.PartidoExternoRepository;
import com.formula11.persistence.PendienteVinculacionRepository;
import com.formula11.service.metricas.GuardadoPartidoService;
import com.formula11.service.metricas.JugadorVinculacionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GuardadoPartidoServiceTest {
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 4);

    private final PartidoExternoRepository partidos = mock(PartidoExternoRepository.class);
    private final MetricaPartidoRepository metricas = mock(MetricaPartidoRepository.class);
    private final PendienteVinculacionRepository pendientes = mock(PendienteVinculacionRepository.class);
    private final JugadorVinculacionService vinculacion = mock(JugadorVinculacionService.class);
    private final GuardadoPartidoService service = new GuardadoPartidoService(partidos, metricas, pendientes,
            vinculacion, Clock.fixed(Instant.parse("2026-10-05T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void noGuardaNadaSiElPartidoYaEstabaCargado() {
        when(partidos.existsByFuenteAndIdExterno("WHOSCORED", "77")).thenReturn(true);

        var resultado = service.guardar(resumen(), new MatchCentreParser.Partido(List.of(who(1)), 0, 0, 0));

        assertFalse(resultado.cargado());
        assertEquals(0, resultado.vinculados());
        assertEquals(0, resultado.pendientes());
        verify(partidos, never()).save(any());
        verifyNoInteractions(vinculacion, metricas, pendientes);
    }

    @Test
    void guardaMetricasDeLosVinculadosYDejaPendientesAlResto() {
        when(partidos.save(any(PartidoExterno.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        MatchCentreParser.Jugador vinculado = who(1);
        MatchCentreParser.Jugador sinVincular = who(2);
        when(vinculacion.vincular(vinculado, Liga.LA_LIGA))
                .thenReturn(new JugadorVinculacionService.ResultadoVinculacion(mock(Jugador.class), false));
        when(vinculacion.vincular(sinVincular, Liga.LA_LIGA))
                .thenReturn(new JugadorVinculacionService.ResultadoVinculacion(null, true));

        var resultado = service.guardar(resumen(),
                new MatchCentreParser.Partido(List.of(vinculado, sinVincular), 0, 0, 0));

        assertTrue(resultado.cargado());
        assertEquals(1, resultado.vinculados());
        assertEquals(1, resultado.pendientes());
        ArgumentCaptor<MetricaPartido> metrica = ArgumentCaptor.forClass(MetricaPartido.class);
        verify(metricas).save(metrica.capture());
        assertEquals(FECHA, metrica.getValue().getFechaPartido());
        verify(pendientes).save(any(PendienteVinculacion.class));
    }

    @Test
    void noDuplicaUnPendienteQueYaExiste() {
        when(partidos.save(any(PartidoExterno.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        MatchCentreParser.Jugador sinVincular = who(2);
        when(vinculacion.vincular(sinVincular, Liga.LA_LIGA))
                .thenReturn(new JugadorVinculacionService.ResultadoVinculacion(null, true));
        when(pendientes.existsByFuenteAndWhoscoredIdAndResueltoFalse(eq("WHOSCORED"), anyString())).thenReturn(true);

        var resultado = service.guardar(resumen(), new MatchCentreParser.Partido(List.of(sinVincular), 0, 0, 0));

        assertEquals(0, resultado.vinculados());
        assertEquals(1, resultado.pendientes());
        verify(pendientes, never()).save(any());
    }

    private static PartidoWhoScoredResumen resumen() {
        return new PartidoWhoScoredResumen(77, ZonedDateTime.of(FECHA.atTime(16, 0), ZoneOffset.UTC),
                "Local", "Visitante", "FT", Liga.LA_LIGA);
    }

    private static MatchCentreParser.Jugador who(long playerId) {
        return new MatchCentreParser.Jugador(playerId, "Jugador " + playerId, "Equipo", true, 90,
                1, 1, 1, 1, 1, 0, BigDecimal.valueOf(7), 0, 0, 0, 0, 0, 0);
    }
}
