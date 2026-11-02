package com.formula11.service;

import com.formula11.model.Jugador;

import java.time.LocalDate;
import java.util.List;

/** Página del ranking de una estrategia en su última fecha de cotización. */
public record RankingResultado(String estrategia, Integer version, LocalDate fecha, int page, int size,
                               long totalElements, int totalPages, List<Item> items) {
    public RankingResultado {
        items = List.copyOf(items);
    }

    public static RankingResultado vacio(String estrategia, int page, int size) {
        return new RankingResultado(estrategia, null, null, page, size, 0, 0, List.of());
    }

    public record Item(int puesto, Jugador jugador, CotizacionLeida cotizacion) {
    }
}
