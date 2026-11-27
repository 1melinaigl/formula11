package com.formula11.service.cotizaciones;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Component;

@Component
public class CotizacionCacheInvalidator {
    private final Cache<String, CotizacionVigentePort.CotizacionVigente> cotizacionCache;
    private final Cache<RankingClave, RankingResultado> rankingCache;

    public CotizacionCacheInvalidator(
            Cache<String, CotizacionVigentePort.CotizacionVigente> cotizacionCache,
            Cache<RankingClave, RankingResultado> rankingCache) {
        this.cotizacionCache = cotizacionCache;
        this.rankingCache = rankingCache;
    }

    public void invalidate() {
        cotizacionCache.invalidateAll();
        rankingCache.invalidateAll();
    }
}
