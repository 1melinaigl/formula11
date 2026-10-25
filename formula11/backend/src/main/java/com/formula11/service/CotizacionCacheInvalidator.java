package com.formula11.service;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Component;

@Component
public class CotizacionCacheInvalidator {
    private final Cache<String, CotizacionVigentePort.Cotizacion> cache;
    public CotizacionCacheInvalidator(Cache<String, CotizacionVigentePort.Cotizacion> cache) { this.cache = cache; }
    public void invalidate() { cache.invalidateAll(); }
}
