package com.formula11.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.persistence.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
public class CotizacionVigenteService implements CotizacionVigentePort {
    private final CotizacionRepository cotizaciones;
    private final EstrategiaRepository estrategias;
    private final Clock clock;
    private final Cache<String, CotizacionVigentePort.Cotizacion> cache;

    public CotizacionVigenteService(CotizacionRepository cotizaciones, EstrategiaRepository estrategias,
                                    Clock clock, Cache<String, CotizacionVigentePort.Cotizacion> cache) {
        this.cotizaciones = cotizaciones;
        this.estrategias = estrategias;
        this.clock = clock;
        this.cache = cache;
    }

    @Override
    public Cotizacion vigente(Jugador jugador, Instant fecha) {
        LocalDate fechaLocal = LocalDate.ofInstant(fecha, clock.getZone());
        String key = jugador.getId() + ":" + fechaLocal;
        CotizacionVigentePort.Cotizacion cached = cache.getIfPresent(key);
        if (cached != null) return cached;
        String codigo = estrategias.findByActivaTrue().map(Estrategia::getCodigo).orElse(null);
        com.formula11.model.Cotizacion value = codigo == null ? null :
                cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                        jugador.getId(), codigo, fechaLocal).orElse(null);
        CotizacionVigentePort.Cotizacion result = value == null
                ? new CotizacionVigentePort.Cotizacion(BigDecimal.ONE, null, null, null)
                : toCotizacion(value);
        cache.put(key, result);
        return result;
    }

    private CotizacionVigentePort.Cotizacion toCotizacion(com.formula11.model.Cotizacion cotizacion) {
        return new CotizacionVigentePort.Cotizacion(cotizacion.getPrecio(), cotizacion.getEstrategia(),
                cotizacion.getVersion(), cotizacion.getFecha());
    }
}
