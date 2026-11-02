package com.formula11.dto;

import com.formula11.service.CotizacionLeida;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Esquema {@code Cotizacion} del contrato. */
public record CotizacionResponse(Long jugadorId, LocalDate fecha, BigDecimal precio, BigDecimal score,
                                 BigDecimal variacion, String estrategia, Integer version) {
    public static CotizacionResponse from(CotizacionLeida cotizacion) {
        if (cotizacion == null) {
            return null;
        }
        return new CotizacionResponse(cotizacion.jugadorId(), cotizacion.fecha(), cotizacion.precio(),
                cotizacion.score(), cotizacion.variacion(), cotizacion.estrategia(), cotizacion.version());
    }
}
