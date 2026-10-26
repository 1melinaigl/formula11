package com.formula11.model.valuacion;

import java.util.Map;
import java.util.Set;

public record ResultadoValuacion(Map<Long, Double> scores, Map<Long, Double> scoresBrutos,
                                 Set<Long> filasSinMinutos) {
    public ResultadoValuacion {
        scores = Map.copyOf(scores);
        scoresBrutos = Map.copyOf(scoresBrutos);
        filasSinMinutos = Set.copyOf(filasSinMinutos);
    }
}
