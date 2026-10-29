package com.formula11.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.WhoScoredArchivoAdapter;
import com.formula11.adapter.WhoScoredPlaywrightAdapter;
import com.formula11.adapter.WhoScoredPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Define la única implementación de {@link WhoScoredPort} según app.whoscored.modo.
 * El adapter de Playwright no abre el navegador al crearse, sino en la primera carga,
 * así que en modo archivos (tests y respaldo sin red) nunca se usa un navegador.
 */
@Configuration
public class WhoScoredConfig {
    static final String MODO_ARCHIVOS = "archivos";

    @Bean
    public WhoScoredPort whoScoredPort(WhoScoredProperties properties, ObjectMapper objectMapper) {
        if (MODO_ARCHIVOS.equalsIgnoreCase(properties.getModo())) {
            return new WhoScoredArchivoAdapter(properties, objectMapper);
        }
        return new WhoScoredPlaywrightAdapter(properties, objectMapper);
    }
}
