package com.formula11.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.formula11.model.*;
import com.formula11.persistence.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CotizacionVigenteService implements CotizacionVigentePort {
    private final CotizacionRepository cotizaciones;
    private final EstrategiaRepository estrategias;
    private final Cache<String, CotizacionVigente> cache;

    public CotizacionVigenteService(CotizacionRepository cotizaciones, EstrategiaRepository estrategias, Cache<String, CotizacionVigente> cache) {
        this.cotizaciones = cotizaciones; this.estrategias = estrategias; this.cache = cache;
    }
    @Override
    public CotizacionVigente obtener(Long jugadorId, LocalDate fecha) {
        String key = jugadorId + ":" + fecha;
        CotizacionVigente cached = cache.getIfPresent(key);
        if (cached != null) return cached;
        String codigo = estrategias.findByActivaTrue().map(Estrategia::getCodigo).orElse(null);
        CotizacionVigente value = codigo == null ? inicial(jugadorId) :
                cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(jugadorId, codigo, fecha)
                        .map(c -> new CotizacionVigente(jugadorId, c.getPrecio(), c.getFecha(), c.getEstrategia(), c.getVersion()))
                        .orElseGet(() -> inicial(jugadorId));
        cache.put(key, value);
        return value;
    }
    private CotizacionVigente inicial(Long id) { return new CotizacionVigente(id, BigDecimal.ONE, null, null, null); }
}
