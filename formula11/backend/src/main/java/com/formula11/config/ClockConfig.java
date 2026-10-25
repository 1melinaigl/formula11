package com.formula11.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.*;

@Configuration
public class ClockConfig {
    @Bean
    public Clock clock(@Value("${app.clock.fecha-fija:}") String fechaFija) {
        ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        if (fechaFija != null && !fechaFija.isBlank()) {
            return Clock.fixed(LocalDate.parse(fechaFija).atStartOfDay(zone).toInstant(), zone);
        }
        return Clock.system(zone);
    }
}
