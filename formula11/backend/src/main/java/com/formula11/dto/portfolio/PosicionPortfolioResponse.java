package com.formula11.dto.portfolio;

import com.formula11.service.portfolio.PosicionPortfolioResultado;
import com.formula11.dto.jugadores.JugadorResponse;
import java.math.BigDecimal;

public record PosicionPortfolioResponse(
        JugadorResponse jugador,
        int cantidad,
        BigDecimal precioPromedio,
        BigDecimal precioActual,
        BigDecimal invertido,
        BigDecimal valorActual,
        BigDecimal ganancia,
        BigDecimal gananciaPorcentaje) {
    public static PosicionPortfolioResponse from(PosicionPortfolioResultado resultado) {
        return new PosicionPortfolioResponse(
                JugadorResponse.from(resultado.jugador()),
                resultado.cantidad(),
                resultado.precioPromedio(),
                resultado.precioActual(),
                resultado.invertido(),
                resultado.valorActual(),
                resultado.ganancia(),
                resultado.gananciaPorcentaje());
    }
}
