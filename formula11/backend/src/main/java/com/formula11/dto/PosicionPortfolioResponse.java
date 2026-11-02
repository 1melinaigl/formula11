package com.formula11.dto;

import com.formula11.service.PosicionPortfolioResultado;

public record PosicionPortfolioResponse(
        JugadorResponse jugador,
        int cantidad,
        java.math.BigDecimal precioPromedio,
        java.math.BigDecimal precioActual,
        java.math.BigDecimal invertido,
        java.math.BigDecimal valorActual,
        java.math.BigDecimal ganancia,
        java.math.BigDecimal gananciaPorcentaje) {
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
