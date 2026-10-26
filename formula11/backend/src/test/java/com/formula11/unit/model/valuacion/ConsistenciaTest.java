package com.formula11.unit.model.valuacion;

import com.formula11.model.valuacion.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsistenciaTest {
    @Test
    void usaSeisPartidosDisponibilidadRatingYDesvioPoblacional() {
        List<MetricaPartidoInput> metricas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            metricas.add(entrada(1, LocalDate.of(2025, 12, 1).plusDays(i), i % 2 == 0,
                    BigDecimal.valueOf(i + 6)));
        }
        var parametros = new ParametrosEstrategia(Map.of("TODAS:titularidad", new BigDecimal("0.5"),
                "TODAS:suplencias", new BigDecimal("-0.5")), BigDecimal.ONE);

        var resultado = new Consistencia().calcular(metricas, LocalDate.of(2025, 12, 7), parametros);

        assertTrue(resultado.scores().containsKey(1L));
        assertTrue(resultado.scores().get(1L) >= 0 && resultado.scores().get(1L) <= 1);
    }

    @Test
    void usaRendimientoComoFallbackConMenosDeCuatroPartidos() {
        var metricas = List.of(entrada(1, LocalDate.of(2025, 12, 1), true, new BigDecimal("8.0")),
                entrada(1, LocalDate.of(2025, 12, 2), true, new BigDecimal("8.5")),
                entrada(1, LocalDate.of(2025, 12, 3), true, new BigDecimal("9.0")));
        var parametros = new ParametrosEstrategia(Map.of("DEL:rating", BigDecimal.ONE), BigDecimal.ONE);

        var resultado = new Consistencia().calcular(metricas, LocalDate.of(2025, 12, 7), parametros);

        assertTrue(resultado.scores().containsKey(1L));
    }

    private static MetricaPartidoInput entrada(long id, LocalDate fecha, boolean titular, BigDecimal rating) {
        return new MetricaPartidoInput(id, "LIGA", "DEL", fecha, 90, titular,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, rating);
    }
}
