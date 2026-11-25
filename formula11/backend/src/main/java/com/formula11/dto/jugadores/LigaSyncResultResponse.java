package com.formula11.dto.jugadores;

public record LigaSyncResultResponse(
        String liga,
        String estado,
        int creados,
        int actualizados,
        int omitidos,
        String error) {
}
