package com.formula11.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/** Parámetros del detector de subvaluados (feature 010). */
@ConfigurationProperties(prefix = "app.senales")
public record SenalesProperties(
        @DefaultValue("15") BigDecimal umbral,
        @DefaultValue("0.05") BigDecimal ventanaScore,
        @DefaultValue("5") int minimoPares,
        @DefaultValue("20") int maximoPorTipo) {
    public SenalesProperties {
        if (umbral == null || umbral.signum() <= 0) {
            throw new IllegalArgumentException("app.senales.umbral debe ser positivo");
        }
        if (ventanaScore == null || ventanaScore.signum() < 0) {
            throw new IllegalArgumentException("app.senales.ventana-score no puede ser negativa");
        }
        if (minimoPares < 1 || maximoPorTipo < 1) {
            throw new IllegalArgumentException("app.senales.minimo-pares y maximo-por-tipo deben ser al menos 1");
        }
    }
}
