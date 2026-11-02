package com.formula11.unit.model;

import com.formula11.model.senales.MedidorAciertos;
import com.formula11.model.senales.MedidorAciertos.SemanaAMedir;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MedidorAciertosTest {
    private static final LocalDate SEMANA_1 = LocalDate.of(2026, 10, 18);
    private static final LocalDate SEMANA_2 = LocalDate.of(2026, 10, 25);

    private static BigDecimal p(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    void comparaLosSenaladosContraElPromedioDelMercado() {
        // Mercado: +10 %, +20 %, 0 %, -10 % -> promedio +5 %.
        Map<Long, BigDecimal> antes = Map.of(1L, p("1.0"), 2L, p("1.0"), 3L, p("2.0"), 4L, p("1.0"));
        Map<Long, BigDecimal> despues = Map.of(1L, p("1.1"), 2L, p("1.2"), 3L, p("2.0"), 4L, p("0.9"));
        // Señales: jugador 2 (+20 %, acierta) y jugador 4 (-10 %, no acierta).
        MedidorAciertos.Resultado resultado = MedidorAciertos.medir(List.of(
                new SemanaAMedir(SEMANA_1, List.of(2L, 4L), antes, despues)));

        assertEquals(1, resultado.semanas().size());
        MedidorAciertos.SemanaMedida semana = resultado.semanas().get(0);
        assertEquals(SEMANA_1, semana.fecha());
        assertEquals(2, semana.senales());
        assertEquals(new BigDecimal("5.00"), semana.variacionSenalados());
        assertEquals(new BigDecimal("5.00"), semana.variacionMercado());
        assertEquals(1, semana.aciertos());
        assertEquals(new BigDecimal("50.00"), resultado.tasaAcierto());
        assertEquals(new BigDecimal("0.00"), resultado.diferenciaPromedio());
    }

    @Test
    void acumulaVariasSemanasOrdenadasPorFecha() {
        Map<Long, BigDecimal> antes = Map.of(1L, p("1.0"), 2L, p("1.0"));
        Map<Long, BigDecimal> despues = Map.of(1L, p("1.3"), 2L, p("1.1"));
        // Mercado +20 %; semana 2 señala al 1 (+30 %, acierta); semana 1 señala al 2 (+10 %, no acierta).
        MedidorAciertos.Resultado resultado = MedidorAciertos.medir(List.of(
                new SemanaAMedir(SEMANA_2, List.of(1L), antes, despues),
                new SemanaAMedir(SEMANA_1, List.of(2L), antes, despues)));

        assertEquals(List.of(SEMANA_1, SEMANA_2), resultado.semanas().stream().map(MedidorAciertos.SemanaMedida::fecha).toList());
        assertEquals(new BigDecimal("50.00"), resultado.tasaAcierto());
        // (+10 - 20) y (+30 - 20): promedio 0.
        assertEquals(new BigDecimal("0.00"), resultado.diferenciaPromedio());
        assertEquals(new BigDecimal("30.00"), resultado.semanas().get(1).variacionSenalados());
    }

    @Test
    void ignoraSenalesSinPrecioYSemanasSinDatos() {
        Map<Long, BigDecimal> antes = Map.of(1L, p("1.0"));
        Map<Long, BigDecimal> despues = Map.of(1L, p("1.5"));

        MedidorAciertos.Resultado resultado = MedidorAciertos.medir(List.of(
                new SemanaAMedir(SEMANA_1, List.of(99L), antes, despues),
                new SemanaAMedir(SEMANA_2, List.of(1L), Map.of(), Map.of())));

        assertTrue(resultado.semanas().isEmpty());
        assertEquals(new BigDecimal("0.00"), resultado.tasaAcierto());
        assertEquals(new BigDecimal("0.00"), resultado.diferenciaPromedio());
    }

    @Test
    void sinSemanasDevuelveCeros() {
        MedidorAciertos.Resultado resultado = MedidorAciertos.medir(List.of());

        assertTrue(resultado.semanas().isEmpty());
        assertEquals(new BigDecimal("0.00"), resultado.tasaAcierto());
    }
}
