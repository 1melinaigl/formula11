package com.formula11.dto;

import com.formula11.service.PortfolioResultado;
import java.util.List;

public record PortfolioResponse(
        Long usuarioId,
        java.time.LocalDate fechaValuacion,
        java.math.BigDecimal saldo,
        java.math.BigDecimal invertido,
        java.math.BigDecimal valorTenencias,
        java.math.BigDecimal ganancia,
        java.math.BigDecimal gananciaPorcentaje,
        List<PosicionPortfolioResponse> posiciones) {
    public static PortfolioResponse from(PortfolioResultado resultado) {
        return new PortfolioResponse(
                resultado.usuario().getId(),
                resultado.fechaValuacion(),
                resultado.saldo(),
                resultado.invertido(),
                resultado.valorTenencias(),
                resultado.ganancia(),
                resultado.gananciaPorcentaje(),
                resultado.posiciones().stream().map(PosicionPortfolioResponse::from).toList());
    }
}
