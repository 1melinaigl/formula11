package com.formula11.dto.cotizaciones;

import com.formula11.service.cotizaciones.EstrategiaResultado;

import java.time.Instant;
import java.util.Map;

public record EstrategiaResponse(String codigo, String nombre, int version, boolean activa,
                                 Map<String, Map<String, Double>> pesos, Map<String, Double> parametros,
                                 Instant creadaEn) {
    public static EstrategiaResponse from(EstrategiaResultado estrategia) {
        return new EstrategiaResponse(estrategia.codigo(), estrategia.nombre(), estrategia.version(), estrategia.activa(),
                estrategia.pesos(), estrategia.parametros(), estrategia.creadaEn());
    }
}
