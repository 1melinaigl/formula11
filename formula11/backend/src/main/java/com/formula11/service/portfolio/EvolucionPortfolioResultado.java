package com.formula11.service.portfolio;

import com.formula11.model.Usuario;
import java.util.List;

public record EvolucionPortfolioResultado(
        Usuario usuario,
        List<PuntoEvolucionResultado> puntos) {
    public EvolucionPortfolioResultado {
        puntos = List.copyOf(puntos);
    }
}
