package com.formula11.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Configuration
public class ClockConfig {

    /** Zona del negocio: define las semanas (lunes a domingo) y las fechas de cotización. */
    public static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Argentina/Buenos_Aires");

    @Bean
    public Clock clock(@Value("${app.clock.fecha-fija:}") String fechaFija) {
        if (fechaFija != null && !fechaFija.isBlank()) {
            return Clock.fixed(LocalDate.parse(fechaFija).atStartOfDay(ZONA_NEGOCIO).toInstant(), ZoneOffset.UTC);
        }
        return Clock.systemUTC();
    }
}