package com.formula11.dto;

import com.formula11.model.Jugador;

public record JugadorResponse(Long id, String nombre, String equipo, String liga, String posicion) {
    public static JugadorResponse from(Jugador jugador) {
        return new JugadorResponse(jugador.getId(), jugador.getNombre(), jugador.getEquipo(), jugador.getLiga().getNombreLegible(), jugador.getPosicion());
    }
}
