package com.formula11.model.senales;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Mide si los jugadores señalados subieron más que el mercado una semana después.
 * Regla de dominio pura: sin Spring ni base de datos.
 */
public final class MedidorAciertos {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private MedidorAciertos() {
    }

    /**
     * Una semana con señales. {@code jugadoresSenalados} trae un id por señal (un jugador con dos señales aparece
     * dos veces). Los mapas son el precio de cada jugador esa semana y la siguiente, en la estrategia de las señales.
     */
    public record SemanaAMedir(LocalDate fecha, List<Long> jugadoresSenalados,
                               Map<Long, BigDecimal> preciosSemana, Map<Long, BigDecimal> preciosSiguiente) {
    }

    public record SemanaMedida(LocalDate fecha, int senales, BigDecimal variacionSenalados,
                               BigDecimal variacionMercado, int aciertos) {
    }

    public record Resultado(List<SemanaMedida> semanas, BigDecimal tasaAcierto, BigDecimal diferenciaPromedio) {
    }

    public static Resultado medir(List<SemanaAMedir> semanas) {
        List<SemanaMedida> medidas = new ArrayList<>();
        int evaluadas = 0;
        int aciertosTotales = 0;
        BigDecimal sumaDiferencias = BigDecimal.ZERO;
        for (SemanaAMedir semana : semanas.stream().sorted(Comparator.comparing(SemanaAMedir::fecha)).toList()) {
            List<BigDecimal> mercado = variaciones(semana.preciosSemana().keySet().stream().toList(), semana);
            List<BigDecimal> senalados = variaciones(semana.jugadoresSenalados(), semana);
            if (mercado.isEmpty() || senalados.isEmpty()) {
                continue;
            }
            BigDecimal variacionMercado = promedio(mercado);
            int aciertos = (int) senalados.stream().filter(v -> v.compareTo(variacionMercado) > 0).count();
            for (BigDecimal variacion : senalados) {
                sumaDiferencias = sumaDiferencias.add(variacion.subtract(variacionMercado));
            }
            evaluadas += senalados.size();
            aciertosTotales += aciertos;
            medidas.add(new SemanaMedida(semana.fecha(), senalados.size(), redondear(promedio(senalados)),
                    redondear(variacionMercado), aciertos));
        }
        if (evaluadas == 0) {
            return new Resultado(List.of(), CERO, CERO);
        }
        BigDecimal cantidad = BigDecimal.valueOf(evaluadas);
        BigDecimal tasa = BigDecimal.valueOf(aciertosTotales).multiply(CIEN).divide(cantidad, 2, RoundingMode.HALF_UP);
        BigDecimal diferencia = sumaDiferencias.divide(cantidad, 2, RoundingMode.HALF_UP);
        return new Resultado(List.copyOf(medidas), tasa, diferencia);
    }

    /** Variación porcentual a una semana de cada jugador con precio positivo en las dos semanas. */
    private static List<BigDecimal> variaciones(List<Long> jugadores, SemanaAMedir semana) {
        List<BigDecimal> resultado = new ArrayList<>();
        for (Long jugadorId : jugadores) {
            BigDecimal antes = semana.preciosSemana().get(jugadorId);
            BigDecimal despues = semana.preciosSiguiente().get(jugadorId);
            if (antes != null && despues != null && antes.signum() > 0) {
                resultado.add(despues.divide(antes, MathContext.DECIMAL64).subtract(BigDecimal.ONE).multiply(CIEN));
            }
        }
        return resultado;
    }

    private static BigDecimal promedio(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valores.size()), MathContext.DECIMAL64);
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
