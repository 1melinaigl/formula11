package com.formula11.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.formula11.adapter.FootballApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FootballCacheConfig {
    @Bean
    Cache<String, Object> footballApiCache(FootballApiProperties properties) {
        return Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtl())
                .maximumSize(properties.getCacheMaximumSize())
                .build();
    }
}
