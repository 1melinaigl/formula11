package com.formula11.service.cotizaciones;

import com.github.benmanes.caffeine.cache.Cache;
import com.formula11.config.ClockConfig;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.persistence.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import com.formula11.model.Cotizacion;

@Service
public class CotizacionVigenteService implements CotizacionVigentePort {
    private final CotizacionRepository cotizaciones;
    private final EstrategiaRepository estrategias;
    private final Cache<String, CotizacionVigentePort.CotizacionVigente> cache;

    public CotizacionVigenteService(CotizacionRepository cotizaciones, EstrategiaRepository estrategias,
                                    Cache<String, CotizacionVigentePort.CotizacionVigente> cache) {
        this.cotizaciones = cotizaciones;
        this.estrategias = estrategias;
        this.cache = cache;
    }

    @Override
    public CotizacionVigentePort.CotizacionVigente vigente(Jugador jugador, Instant fecha) {
        LocalDate fechaLocal = LocalDate.ofInstant(fecha, ClockConfig.ZONA_NEGOCIO);
        String key = jugador.getId() + ":" + fechaLocal;
        CotizacionVigentePort.CotizacionVigente cached = cache.getIfPresent(key);
        if (cached != null) return cached;
        String codigo = estrategias.findByActivaTrue().map(Estrategia::getCodigo).orElse(null);
        Cotizacion value = codigo == null ? null :
                cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                        jugador.getId(), codigo, fechaLocal).orElse(null);
        CotizacionVigentePort.CotizacionVigente result = value == null
                ? new CotizacionVigentePort.CotizacionVigente(BigDecimal.ONE, null, null, null)
                : toCotizacion(value);
        cache.put(key, result);
        return result;
    }

    private CotizacionVigentePort.CotizacionVigente toCotizacion(Cotizacion cotizacion) {
        return new CotizacionVigentePort.CotizacionVigente(cotizacion.getPrecio(), cotizacion.getEstrategia(),
                cotizacion.getVersion(), cotizacion.getFecha());
    }
}
