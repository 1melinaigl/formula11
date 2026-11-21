package com.formula11.dto.jugadores;

import java.util.List;

public record JugadoresPageResponse(
        List<JugadorResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
