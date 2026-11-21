package com.formula11.dto.jugadores;

import java.util.List;

public record SincronizacionResponse(
        String estado,
        List<LigaSyncResultResponse> ligas,
        int creados,
        int actualizados,
        int omitidos,
        String correlationId) {
}
