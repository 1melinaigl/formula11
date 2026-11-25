package com.formula11.dto.portfolio;

import java.util.List;

public record EvolucionPortfolioResponse(Long usuarioId, List<PuntoEvolucionResponse> puntos) {
    public EvolucionPortfolioResponse {
        puntos = List.copyOf(puntos);
    }
}
