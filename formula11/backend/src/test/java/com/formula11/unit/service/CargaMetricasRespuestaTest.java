package com.formula11.unit.service;

import com.formula11.adapter.WhoScoredPort;
import com.formula11.config.WhoScoredProperties;
import com.formula11.dto.EjecucionMetricasResponse;
import com.formula11.dto.PendientesPageResponse;
import com.formula11.model.EjecucionMetricaLiga;
import com.formula11.model.EjecucionMetricas;
import com.formula11.model.Liga;
import com.formula11.model.PendienteVinculacion;
import com.formula11.persistence.EjecucionMetricaLigaRepository;
import com.formula11.persistence.EjecucionMetricasRepository;
import com.formula11.persistence.PendienteVinculacionRepository;
import com.formula11.service.CargaMetricasService;
import com.formula11.service.CargaPartidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Las respuestas de métricas informan la liga con su nombre legible, como el resto del contrato. */
class CargaMetricasRespuestaTest {
    private static final Instant AHORA = Instant.parse("2026-10-30T12:00:00Z");

    @SuppressWarnings("unchecked")
    private static CargaMetricasService service(PendienteVinculacionRepository pendientes) {
        return new CargaMetricasService(mock(WhoScoredPort.class), new WhoScoredProperties(), mock(CargaPartidoService.class),
                mock(EjecucionMetricasRepository.class), mock(EjecucionMetricaLigaRepository.class), pendientes,
                Clock.fixed(AHORA, ZoneOffset.UTC), mock(ObjectProvider.class));
    }

    @Test
    void elResumenPorLigaUsaElNombreLegible() {
        EjecucionMetricas ejecucion = new EjecucionMetricas("COMPLETADA", AHORA, "corr");
        EjecucionMetricaLiga laLiga = new EjecucionMetricaLiga(ejecucion, Liga.LA_LIGA);

        EjecucionMetricasResponse respuesta = service(mock(PendienteVinculacionRepository.class))
                .response(ejecucion, List.of(laLiga));

        assertEquals("La Liga", respuesta.ligas().get(0).liga());
    }

    @Test
    void losPendientesUsanElNombreLegible() {
        PendienteVinculacionRepository repository = mock(PendienteVinculacionRepository.class);
        PendienteVinculacion pendiente = new PendienteVinculacion("WHOSCORED", "123", "Jugador", "Equipo",
                Liga.SERIE_A, AHORA);
        when(repository.findByResueltoFalse(any())).thenReturn(new PageImpl<>(List.of(pendiente), PageRequest.of(0, 20), 1));

        PendientesPageResponse respuesta = service(repository).pendientes(PageRequest.of(0, 20));

        assertEquals("Serie A", respuesta.content().get(0).liga());
    }
}
