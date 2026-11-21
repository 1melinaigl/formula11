package com.formula11.dto.cotizaciones;

import com.formula11.model.Jugador;
import com.formula11.service.cotizaciones.JugadorDetalleResultado;

/** Esquema {@code JugadorDetalleResponse}: los campos de {@code JugadorResponse} más la cotización actual. */
public record JugadorDetalleResponse(Long id, String nombre, String equipo, String liga, String posicion,
                                     CotizacionResponse cotizacionActual) {
    public static JugadorDetalleResponse from(JugadorDetalleResultado resultado) {
        Jugador jugador = resultado.jugador();
        return new JugadorDetalleResponse(jugador.getId(), jugador.getNombre(), jugador.getEquipo(),
                jugador.getLiga().getNombreLegible(), jugador.getPosicion(),
                CotizacionResponse.from(resultado.cotizacionActual()));
    }
}
