package com.formula11.service;

import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación real de {@link CotizacionHistoricaPort} (feature 008), consumida por la evolución del portfolio (009).
 * Usa siempre la estrategia activa al momento de la consulta.
 */
@Service
@Transactional(readOnly = true)
public class CotizacionHistoricaService implements CotizacionHistoricaPort {
    static final BigDecimal PRECIO_INICIAL = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);

    private final CotizacionRepository cotizaciones;
    private final EstrategiaRepository estrategias;

    public CotizacionHistoricaService(CotizacionRepository cotizaciones, EstrategiaRepository estrategias) {
        this.cotizaciones = cotizaciones;
        this.estrategias = estrategias;
    }

    @Override
    public List<LocalDate> fechasCotizacion(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            return List.of();
        }
        return estrategiaActiva()
                .map(codigo -> List.copyOf(cotizaciones.findFechasByEstrategiaBetween(codigo, desde, hasta)))
                .orElse(List.of());
    }

    @Override
    public Map<Long, BigDecimal> preciosA(Collection<Long> jugadorIds, LocalDate fecha) {
        if (jugadorIds == null || jugadorIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = new LinkedHashSet<>(jugadorIds);
        ids.remove(null);
        Map<Long, BigDecimal> precios = new LinkedHashMap<>();
        Optional<String> activa = estrategiaActiva();
        if (activa.isPresent() && fecha != null && !ids.isEmpty()) {
            for (Cotizacion cotizacion : cotizaciones.findVigentesA(activa.get(), ids, fecha)) {
                precios.put(cotizacion.getJugador().getId(), cotizacion.getPrecio().setScale(4, RoundingMode.HALF_UP));
            }
        }
        ids.forEach(id -> precios.putIfAbsent(id, PRECIO_INICIAL));
        return Map.copyOf(precios);
    }

    private Optional<String> estrategiaActiva() {
        return estrategias.findByActivaTrue().map(Estrategia::getCodigo);
    }
}
