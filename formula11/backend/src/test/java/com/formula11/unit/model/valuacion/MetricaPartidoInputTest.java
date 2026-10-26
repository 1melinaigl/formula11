package com.formula11.unit.model.valuacion;

import com.formula11.model.valuacion.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MetricaPartidoInputTest {
    @Test
    void convierteConteosAusentesEnCeroYConservaFilaSinMinutos() {
        var input = new MetricaPartidoInput(1, "LIGA", "DEL", LocalDate.of(2026, 1, 1),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(0, input.goles());
        assertEquals(0, input.tiros());
        assertFalse(input.jugo());
    }

    @Test
    void redistribuyeElPesoDelRatingAusenteYDevuelveFilaExcluida() {
        var metricas = java.util.List.of(
                new MetricaPartidoInput(1, "LIGA", "DEL", LocalDate.of(2026, 1, 1),
                        90, true, 2, null, null, null, null, null, null, null, null, null, null, null, null),
                new MetricaPartidoInput(2, "LIGA", "DEL", LocalDate.of(2026, 1, 1),
                        null, null, 1, null, null, null, null, null, null, null, null, null, null, null, null));
        var parametros = new ParametrosEstrategia(java.util.Map.of("DEL:goles", new java.math.BigDecimal("0.5"),
                "DEL:rating", new java.math.BigDecimal("0.5")), java.math.BigDecimal.ONE);

        var resultado = new RendimientoPosicion().calcular(metricas, LocalDate.of(2026, 1, 4), parametros);

        assertTrue(resultado.scores().containsKey(1L));
        assertTrue(resultado.filasSinMinutos().contains(2L));
    }
}
