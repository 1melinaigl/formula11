package com.formula11.adapter;

import com.formula11.service.CotizacionHistoricaPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CotizacionHistoricaFijaAdapter implements CotizacionHistoricaPort {
    private static final BigDecimal PRECIO_FIJO = new BigDecimal("1.0000");

    @Override
    public List<LocalDate> fechasCotizacion(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            return List.of();
        }
        LocalDate lunes = desde.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        List<LocalDate> fechas = new java.util.ArrayList<>();
        while (!lunes.isAfter(hasta)) {
            fechas.add(lunes);
            lunes = lunes.plusWeeks(1);
        }
        return List.copyOf(fechas);
    }

    @Override
    public Map<Long, BigDecimal> preciosA(Collection<Long> jugadorIds, LocalDate fecha) {
        Map<Long, BigDecimal> precios = new LinkedHashMap<>();
        if (jugadorIds != null) {
            jugadorIds.forEach(id -> precios.put(id, PRECIO_FIJO));
        }
        return Map.copyOf(precios);
    }
}
