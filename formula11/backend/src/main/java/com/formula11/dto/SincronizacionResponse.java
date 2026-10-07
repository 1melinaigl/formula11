package com.formula11.dto;

import java.util.List;

public record SincronizacionResponse(
        String estado,
        List<LigaSyncResultResponse> ligas,
        int creados,
        int actualizados,
        int omitidos,
        String correlationId) {
}
