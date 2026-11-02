package com.formula11.unit.model.valuacion;

import com.formula11.model.valuacion.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RendimientoPosicionTest {
    @Test
    void promediaEstandarizaYNormalizaPorPosicion() {
        var metricas = List.of(
                entrada(1, "DEL", 2, 8.0),
                entrada(1, "DEL", 4, 8.0),
                entrada(2, "DEL", 0, 6.0),
                entrada(2, "DEL", 0, 6.0),
                entrada(3, "DEF", 3, 7.0));
        var pesos = new ParametrosEstrategia(Map.of("DEL:goles", new BigDecimal("1.0"),
                "DEL:rating", new BigDecimal("0.5")), BigDecimal.ONE);

        var resultado = new RendimientoPosicion().calcular(metricas, LocalDate.of(2026, 1, 4), pesos);

        assertTrue(resultado.scores().get(1L) > resultado.scores().get(2L));
        assertEquals(0.5, resultado.scores().get(3L), 0.00001);
        assertTrue(resultado.scores().values().stream().allMatch(s -> s >= 0 && s <= 1));
    }

    @Test
    void incluyeLaProporcionDePartidosConMenosDeNoventaMinutos() {
        var metricas = List.of(entrada(1, "DEL", 60, 0, 7.0),
                entrada(2, "DEL", 90, 0, 7.0));
        var pesos = new ParametrosEstrategia(Map.of("DEL:menosDe90Minutos", new BigDecimal("-1.0")),
                BigDecimal.ONE);

        var resultado = new RendimientoPosicion().calcular(metricas, LocalDate.of(2026, 1, 4), pesos);

        assertTrue(resultado.scores().get(1L) < resultado.scores().get(2L));
    }

    private static MetricaPartidoInput entrada(long id, String posicion, int goles, double rating) {
        return entrada(id, posicion, 90, goles, rating);
    }

    private static MetricaPartidoInput entrada(long id, String posicion, int minutos,
                                               int goles, double rating) {
        return new MetricaPartidoInput(id, "LIGA", posicion, LocalDate.of(2026, 1, 1),
                minutos, true, goles,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                BigDecimal.valueOf(rating));
    }
}
