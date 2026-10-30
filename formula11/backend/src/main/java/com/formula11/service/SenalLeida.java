package com.formula11.service;

import com.formula11.model.Jugador;
import com.formula11.model.Senal;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Señal lista para la API, con el jugador ya cargado. */
public record SenalLeida(Long id, Jugador jugador, LocalDate fecha, Senal.Tipo tipo, BigDecimal brecha,
                         BigDecimal precio, BigDecimal precioEstimado) {
    public static SenalLeida de(Senal senal) {
        return new SenalLeida(senal.getId(), senal.getJugador(), senal.getFecha(), senal.getTipo(), senal.getBrecha(),
                senal.getPrecio(), senal.getPrecioEstimado());
    }
}
