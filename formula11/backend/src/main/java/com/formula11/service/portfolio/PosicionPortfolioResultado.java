package com.formula11.service.portfolio;

import com.formula11.model.jugadores.Jugador;
import java.math.BigDecimal;

public record PosicionPortfolioResultado(
        Jugador jugador,
        int cantidad,
        BigDecimal precioPromedio,
        BigDecimal precioActual,
        BigDecimal invertido,
        BigDecimal valorActual,
        BigDecimal ganancia,
        BigDecimal gananciaPorcentaje) {
}
