package com.formula11.unit.service.cotizaciones;

import com.github.benmanes.caffeine.cache.Cache;
import com.formula11.service.cotizaciones.RankingClave;
import com.formula11.service.cotizaciones.RankingResultado;
import com.formula11.service.cotizaciones.CotizacionCacheInvalidator;
import com.formula11.service.cotizaciones.CotizacionVigentePort;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class CotizacionCacheInvalidatorTest {
    @Test
    void invalidaAmbasCaches() {
        Cache<String, CotizacionVigentePort.CotizacionVigente> cotizacionCache = mock(Cache.class);
        Cache<RankingClave, RankingResultado> rankingCache = mock(Cache.class);

        new CotizacionCacheInvalidator(cotizacionCache, rankingCache).invalidate();

        verify(cotizacionCache).invalidateAll();
        verify(rankingCache).invalidateAll();
    }
}
