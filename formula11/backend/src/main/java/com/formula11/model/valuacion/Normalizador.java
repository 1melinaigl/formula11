package com.formula11.model.valuacion;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/** The two normalization levels used by valuation strategies. */
public final class Normalizador {
    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    private Normalizador() {
    }

    /** Unbounded z-score. A constant (or one-element) group has z-score zero. */
    public static double zScore(double value, List<Double> group) {
        if (group == null || group.isEmpty()) return 0d;
        double mean = group.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        double variance = group.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0d);
        double deviation = Math.sqrt(variance);
        return deviation == 0d ? 0d : (value - mean) / deviation;
    }

    public static double zScore(double value, double mean, double deviation) {
        return deviation == 0d ? 0d : (value - mean) / deviation;
    }

    public static double normalizarMetrica(double value, double mean, double deviation) {
        return zScore(value, mean, deviation);
    }

    /** Final score: z-score clipped to [-2, 2], then mapped to [0, 1]. */
    public static double scoreFinal(double value, List<Double> group) {
        if (group == null || group.size() < 2) return 0.5d;
        double mean = group.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        double variance = group.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0d);
        double deviation = Math.sqrt(variance);
        if (deviation == 0d) return 0.5d;
        double z = Math.max(-2d, Math.min(2d, (value - mean) / deviation));
        return (z + 2d) / 4d;
    }

    public static BigDecimal scoreFinal(BigDecimal value, List<BigDecimal> group) {
        return BigDecimal.valueOf(scoreFinal(value.doubleValue(),
                group.stream().map(BigDecimal::doubleValue).toList())).round(MC);
    }

    public static double normalizarFinal(double value, List<Double> group) {
        return scoreFinal(value, group);
    }
}
