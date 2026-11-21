package com.formula11.dto.jugadores;

import com.formula11.service.jugadores.PaginaJugadores;

import java.util.List;

public record JugadoresPageResponse(
        List<JugadorResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
    public static JugadoresPageResponse from(PaginaJugadores pagina) {
        return new JugadoresPageResponse(pagina.content().stream().map(JugadorResponse::from).toList(),
                pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages());
    }
}
