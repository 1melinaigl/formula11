package com.formula11.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.formula11.model.CotizacionVigente;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class CotizacionCacheConfig {
    @Bean
    Cache<String, CotizacionVigente> cotizacionVigenteCache() {
        return Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(15)).maximumSize(10_000).build();
    }
}
