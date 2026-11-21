package com.formula11.unit.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.formula11.service.cotizaciones.CotizacionCacheInvalidator;
import com.formula11.service.cotizaciones.CotizacionVigentePort;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class CotizacionCacheInvalidatorTest {
    @Test
    void invalidaTodaLaCache() {
        Cache<String, CotizacionVigentePort.CotizacionVigente> cache = mock(Cache.class);

        new CotizacionCacheInvalidator(cache).invalidate();

        verify(cache).invalidateAll();
    }
}
