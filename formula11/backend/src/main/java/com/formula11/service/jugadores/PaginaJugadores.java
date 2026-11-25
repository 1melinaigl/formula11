package com.formula11.service.jugadores;

import com.formula11.model.jugadores.Jugador;

import java.util.List;

/** Una página del catálogo de jugadores. */
public record PaginaJugadores(List<Jugador> content, int page, int size, long totalElements, int totalPages) {}
