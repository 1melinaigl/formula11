package com.formula11.model.valuacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.LinkedHashMap;

public record ParametrosEstrategia(Map<String, BigDecimal> pesos, BigDecimal lambda) {
    public ParametrosEstrategia {
        pesos = pesos == null ? Map.of() : Map.copyOf(pesos);
        lambda = lambda == null ? BigDecimal.ONE : lambda;
    }

    /** Flattens the JSON-shaped position -> metric weights used by persistence. */
    public static ParametrosEstrategia desde(Map<String, Map<String, BigDecimal>> porPosicion,
                                             BigDecimal lambda) {
        Map<String, BigDecimal> flat = new LinkedHashMap<>();
        porPosicion.forEach((position, values) ->
                values.forEach((metric, weight) -> flat.put(position + ":" + metric, weight)));
        return new ParametrosEstrategia(flat, lambda);
    }
}
