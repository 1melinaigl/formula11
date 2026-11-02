package com.formula11.dto;

import java.time.Instant;
import java.util.Map;

public record EstrategiaResponse(String codigo, String nombre, int version, boolean activa,
                                 Map<String, Map<String, Double>> pesos, Map<String, Double> parametros,
                                 Instant creadaEn) {}
