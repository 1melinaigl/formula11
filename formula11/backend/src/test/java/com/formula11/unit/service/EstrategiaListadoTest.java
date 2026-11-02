package com.formula11.unit.service;

import com.formula11.dto.EstrategiaResponse;
import com.formula11.model.Estrategia;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.service.CotizacionCacheInvalidator;
import com.formula11.service.EstrategiaService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** GET /api/estrategias: las dos estrategias, cada una con su versión vigente y la activa marcada. */
class EstrategiaListadoTest {

    private static Estrategia estrategia(String codigo, int version, boolean activa) {
        return new Estrategia(codigo, codigo, version, Map.of(), Map.of(), activa, Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    void devuelveUnaFilaPorEstrategiaConLaUltimaVersionYLaActivaPorCodigo() {
        EstrategiaRepository repository = mock(EstrategiaRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                estrategia("RENDIMIENTO_POSICION", 1, true),
                estrategia("RENDIMIENTO_POSICION", 2, false),
                estrategia("CONSISTENCIA", 1, false)));
        EstrategiaService service = new EstrategiaService(repository, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                mock(CotizacionCacheInvalidator.class));

        List<EstrategiaResponse> listado = service.listar();

        assertEquals(2, listado.size());
        assertEquals("CONSISTENCIA", listado.get(0).codigo());
        assertFalse(listado.get(0).activa());
        assertEquals("RENDIMIENTO_POSICION", listado.get(1).codigo());
        assertEquals(2, listado.get(1).version());
        assertTrue(listado.get(1).activa());
    }
}
