package com.formula11.unit.service;

import com.formula11.adapter.WhoScoredPort;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.EjecucionMetricas;
import com.formula11.persistence.EjecucionMetricaLigaRepository;
import com.formula11.persistence.EjecucionMetricasRepository;
import com.formula11.service.CargaMetricasService;
import com.formula11.service.CargaPartidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CargaMetricasServiceTest {
    @Test
    void creaEjecucionYDelegalaCargaAsincronica() {
        WhoScoredPort port = mock(WhoScoredPort.class);
        WhoScoredProperties properties = new WhoScoredProperties();
        CargaPartidoService partidos = mock(CargaPartidoService.class);
        EjecucionMetricasRepository ejecuciones = mock(EjecucionMetricasRepository.class);
        EjecucionMetricaLigaRepository ligas = mock(EjecucionMetricaLigaRepository.class);
        ObjectProvider<CargaMetricasService> self = mock(ObjectProvider.class);
        CargaMetricasService service = new CargaMetricasService(port, properties, partidos,
                ejecuciones, ligas, Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC), self);
        EjecucionMetricas ejecucion = new EjecucionMetricas("EN_CURSO",
                Instant.parse("2026-10-28T00:00:00Z"), "corr");
        when(ejecuciones.save(any())).thenReturn(ejecucion);
        CargaMetricasService proxy = mock(CargaMetricasService.class);
        when(self.getObject()).thenReturn(proxy);

        assertSame(ejecucion, service.iniciar(null));
        verify(ejecuciones).save(any(EjecucionMetricas.class));
        verify(self).getObject();
        assertTrue(service.estaEnCurso());
    }

}
