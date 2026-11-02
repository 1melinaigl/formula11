package com.formula11.unit.service;

import com.formula11.dto.RecalculoRequest;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.service.CotizacionCacheInvalidator;
import com.formula11.service.GeneradorSenalesPort;
import com.formula11.service.RecalculoSemanaService;
import com.formula11.service.RecalculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 010: el recálculo genera señales después de cada semana y no falla si la generación falla. */
class RecalculoServiceSenalesTest {
    private static final LocalDate PRIMER_DOMINGO = LocalDate.of(2026, 10, 18);
    private static final LocalDate SEGUNDO_DOMINGO = LocalDate.of(2026, 10, 25);

    private RecalculoSemanaService semanas;
    private GeneradorSenalesPort generador;
    private RecalculoService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        semanas = mock(RecalculoSemanaService.class);
        EstrategiaRepository estrategias = mock(EstrategiaRepository.class);
        CotizacionRepository cotizaciones = mock(CotizacionRepository.class);
        generador = mock(GeneradorSenalesPort.class);
        ObjectProvider<GeneradorSenalesPort> generadores = mock(ObjectProvider.class);
        when(generadores.orderedStream()).thenAnswer(invocation -> Stream.of(generador));
        when(semanas.procesar(any())).thenReturn(new RecalculoSemanaService.SemanaResultado(2, 0));
        when(estrategias.findAll()).thenReturn(List.of());
        when(cotizaciones.findMaxFecha()).thenReturn(Optional.empty());
        service = new RecalculoService(Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC), semanas,
                estrategias, cotizaciones, mock(CotizacionCacheInvalidator.class), generadores);
    }

    @Test
    void generaLasSenalesDeCadaSemanaDespuesDeProcesarla() {
        var respuesta = service.recalcular(new RecalculoRequest(PRIMER_DOMINGO, SEGUNDO_DOMINGO));

        assertEquals(2, respuesta.semanasCalculadas());
        var orden = inOrder(semanas, generador);
        orden.verify(semanas).procesar(PRIMER_DOMINGO);
        orden.verify(generador).generar(PRIMER_DOMINGO);
        orden.verify(semanas).procesar(SEGUNDO_DOMINGO);
        orden.verify(generador).generar(SEGUNDO_DOMINGO);
    }

    @Test
    void siFallaLaGeneracionElRecalculoTerminaIgual() {
        doThrow(new IllegalStateException("falla de señales")).when(generador).generar(any());

        var respuesta = service.recalcular(new RecalculoRequest(PRIMER_DOMINGO, SEGUNDO_DOMINGO));

        assertEquals("COMPLETADO", respuesta.estado());
        assertEquals(2, respuesta.semanasCalculadas());
        verify(generador, times(2)).generar(any());
    }
}
