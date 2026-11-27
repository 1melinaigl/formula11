package com.formula11.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.formula11.service.cotizaciones.RankingClave;
import com.formula11.service.cotizaciones.RankingResultado;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RankingCacheConfig {
    @Bean
    Cache<RankingClave, RankingResultado> rankingCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(1000)
                .build();
    }
}
