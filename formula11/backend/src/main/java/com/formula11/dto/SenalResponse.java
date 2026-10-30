package com.formula11.dto;

import com.formula11.service.SenalLeida;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Esquema {@code Senal} del contrato. */
public record SenalResponse(Long id, JugadorResponse jugador, LocalDate fecha, String tipo, BigDecimal brecha,
                            BigDecimal precio, BigDecimal precioEstimado) {
    public static SenalResponse from(SenalLeida senal) {
        return new SenalResponse(senal.id(), JugadorResponse.from(senal.jugador()), senal.fecha(),
                senal.tipo().name(), senal.brecha(), senal.precio(), senal.precioEstimado());
    }
}
