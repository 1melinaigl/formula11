package com.formula11.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Variación porcentual de una cotización contra la anterior del mismo jugador y estrategia.
 * Regla de dominio pura: sin Spring ni base de datos.
 */
public final class VariacionCotizacion {
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int DECIMALES = 2;

    private VariacionCotizacion() {
    }

    /**
     * Devuelve {@code (actual / anterior - 1) * 100} con 2 decimales HALF_UP,
     * o {@code null} si no hay cotización anterior.
     */
    public static BigDecimal porcentaje(BigDecimal anterior, BigDecimal actual) {
        if (anterior == null) {
            return null;
        }
        if (actual == null) {
            throw new IllegalArgumentException("El precio actual es obligatorio");
        }
        if (anterior.signum() <= 0) {
            throw new IllegalArgumentException("El precio anterior debe ser positivo");
        }
        return actual.divide(anterior, MathContext.DECIMAL64)
                .subtract(BigDecimal.ONE)
                .multiply(CIEN)
                .setScale(DECIMALES, RoundingMode.HALF_UP);
    }
}
