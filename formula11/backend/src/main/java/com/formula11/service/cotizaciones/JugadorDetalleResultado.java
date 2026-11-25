package com.formula11.service.cotizaciones;

import com.formula11.model.jugadores.Jugador;

/** Jugador con su cotización actual de la estrategia activa; {@code cotizacionActual} es null si no tiene. */
public record JugadorDetalleResultado(Jugador jugador, CotizacionLeida cotizacionActual) {
}
