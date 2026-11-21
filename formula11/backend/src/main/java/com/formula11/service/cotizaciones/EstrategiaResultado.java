package com.formula11.service.cotizaciones;

import com.formula11.model.Estrategia;

import java.time.Instant;
import java.util.Map;

/** Una versión de estrategia, con la marca de si su código es el activo. */
public record EstrategiaResultado(String codigo, String nombre, int version, boolean activa,
                                  Map<String, Map<String, Double>> pesos, Map<String, Double> parametros,
                                  Instant creadaEn) {
    static EstrategiaResultado de(Estrategia estrategia, boolean activa) {
        return new EstrategiaResultado(estrategia.getCodigo(), estrategia.getNombre(), estrategia.getVersion(), activa,
                estrategia.getPesos(), estrategia.getParametros(), estrategia.getCreadaEn());
    }

    static EstrategiaResultado de(Estrategia estrategia) {
        return de(estrategia, estrategia.isActiva());
    }
}
