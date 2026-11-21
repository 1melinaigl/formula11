package com.formula11.service.cotizaciones;

import java.util.Map;

/** Pesos por posición y parámetros de precio de una nueva versión de estrategia. */
public record NuevaVersion(Map<String, Map<String, Double>> pesos, Map<String, Double> parametros) {}
