package com.formula11.dto;

import com.formula11.service.HistorialCotizacionesResultado;

import java.util.List;

/** Esquema {@code HistorialCotizacionesResponse}. */
public record HistorialCotizacionesResponse(Long jugadorId, String estrategia, List<CotizacionResponse> cotizaciones) {
    public static HistorialCotizacionesResponse from(HistorialCotizacionesResultado resultado) {
        return new HistorialCotizacionesResponse(resultado.jugadorId(), resultado.estrategia(),
                resultado.cotizaciones().stream().map(CotizacionResponse::from).toList());
    }
}
