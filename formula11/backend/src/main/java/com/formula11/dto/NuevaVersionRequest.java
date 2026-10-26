package com.formula11.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record NuevaVersionRequest(
        @NotEmpty(message = "Los pesos son obligatorios") Map<String, Map<String, Double>> pesos,
        @NotEmpty(message = "Los parámetros son obligatorios") Map<String, Double> parametros) {}
