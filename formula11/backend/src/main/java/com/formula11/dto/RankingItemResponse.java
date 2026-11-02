package com.formula11.dto;

import com.formula11.service.RankingResultado;

import java.math.BigDecimal;

/** Esquema {@code RankingItem}. */
public record RankingItemResponse(int puesto, JugadorResponse jugador, BigDecimal precio, BigDecimal variacion,
                                  BigDecimal score) {
    public static RankingItemResponse from(RankingResultado.Item item) {
        return new RankingItemResponse(item.puesto(), JugadorResponse.from(item.jugador()),
                item.cotizacion().precio(), item.cotizacion().variacion(), item.cotizacion().score());
    }
}
