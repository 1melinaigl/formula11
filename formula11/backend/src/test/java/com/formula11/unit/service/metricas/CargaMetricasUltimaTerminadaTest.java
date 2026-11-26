package com.formula11.unit.service.metricas;

import com.formula11.adapter.whoscored.WhoScoredPort;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.metricas.EjecucionMetricas;
import com.formula11.persistence.metricas.EjecucionMetricaLigaRepository;
import com.formula11.persistence.metricas.EjecucionMetricasRepository;
import com.formula11.persistence.metricas.PendienteVinculacionRepository;
import com.formula11.service.metricas.CargaMetricasService;
import com.formula11.service.metricas.CargaPartidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 016: la última carga terminada, que usa el health indicator de la carga de métricas. */
class CargaMetricasUltimaTerminadaTest {
    private EjecucionMetricasRepository ejecuciones;
    private CargaMetricasService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ejecuciones = mock(EjecucionMetricasRepository.class);
        service = new CargaMetricasService(mock(WhoScoredPort.class), new WhoScoredProperties(),
                mock(CargaPartidoService.class), ejecuciones, mock(EjecucionMetricaLigaRepository.class),
                mock(PendienteVinculacionRepository.class),
                Clock.fixed(Instant.parse("2026-11-26T12:00:00Z"), ZoneOffset.UTC), mock(ObjectProvider.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pideLaUltimaEntreLasTerminadasSinContarLaEnCurso() {
        EjecucionMetricas fallida = new EjecucionMetricas("FALLIDA", Instant.parse("2026-11-23T06:00:00Z"), "c");
        when(ejecuciones.findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc(any())).thenReturn(Optional.of(fallida));

        Optional<EjecucionMetricas> ultima = service.ultimaTerminada();

        assertTrue(ultima.isPresent());
        assertSame(fallida, ultima.get());
        ArgumentCaptor<Collection<String>> estados = ArgumentCaptor.forClass(Collection.class);
        verify(ejecuciones).findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc(estados.capture());
        assertEquals(Set.of("COMPLETADA", "PARCIAL", "FALLIDA"), Set.copyOf(estados.getValue()));
    }

    @Test
    void sinCargasTerminadasDevuelveVacio() {
        when(ejecuciones.findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc(any())).thenReturn(Optional.empty());

        assertTrue(service.ultimaTerminada().isEmpty());
    }
}
