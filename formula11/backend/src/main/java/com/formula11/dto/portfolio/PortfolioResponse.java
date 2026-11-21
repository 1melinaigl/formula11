package com.formula11.dto.portfolio;

import com.formula11.service.portfolio.PortfolioResultado;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioResponse(
        Long usuarioId,
        LocalDate fechaValuacion,
        BigDecimal saldo,
        BigDecimal invertido,
        BigDecimal valorTenencias,
        BigDecimal ganancia,
        BigDecimal gananciaPorcentaje,
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
