package com.formula11.unit.model;

import com.formula11.model.Estrategia;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstrategiaVigentesTest {

    private static Estrategia estrategia(String codigo, int version) {
        return new Estrategia(codigo, codigo, version, Map.of(), Map.of(), false, Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    void devuelveSoloLaUltimaVersionDeCadaCodigo() {
        Estrategia rendimientoV1 = estrategia("RENDIMIENTO_POSICION", 1);
        Estrategia rendimientoV3 = estrategia("RENDIMIENTO_POSICION", 3);
        Estrategia rendimientoV2 = estrategia("RENDIMIENTO_POSICION", 2);
        Estrategia consistenciaV1 = estrategia("CONSISTENCIA", 1);

        List<Estrategia> vigentes = Estrategia.vigentes(List.of(rendimientoV1, consistenciaV1, rendimientoV3, rendimientoV2));

        assertEquals(List.of(rendimientoV3, consistenciaV1), vigentes);
    }

    @Test
    void sinEstrategiasDevuelveListaVacia() {
        assertTrue(Estrategia.vigentes(List.of()).isEmpty());
    }
}
