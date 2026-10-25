package com.formula11.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.formula11.model.CotizacionVigente;
import org.springframework.stereotype.Component;

@Component
public class CotizacionCacheInvalidator {
    private final Cache<String, CotizacionVigente> cache;
    public CotizacionCacheInvalidator(Cache<String, CotizacionVigente> cache) { this.cache = cache; }
    public void invalidate() { cache.invalidateAll(); }
}
