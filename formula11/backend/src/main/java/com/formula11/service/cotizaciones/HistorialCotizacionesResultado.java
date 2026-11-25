package com.formula11.service.cotizaciones;

import java.util.List;

/** Página del historial de cotizaciones de un jugador para una estrategia, ordenada por fecha ascendente. */
public record HistorialCotizacionesResultado(Long jugadorId, String estrategia, List<CotizacionLeida> cotizaciones,
                                             int page, int size, long totalElements, int totalPages) {
    public HistorialCotizacionesResultado {
        cotizaciones = List.copyOf(cotizaciones);
    }
}
