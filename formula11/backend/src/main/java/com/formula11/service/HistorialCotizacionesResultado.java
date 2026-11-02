package com.formula11.service;

import java.util.List;

/** Serie de cotizaciones de un jugador para una estrategia, ordenada por fecha ascendente. */
public record HistorialCotizacionesResultado(Long jugadorId, String estrategia, List<CotizacionLeida> cotizaciones) {
    public HistorialCotizacionesResultado {
        cotizaciones = List.copyOf(cotizaciones);
    }
}
