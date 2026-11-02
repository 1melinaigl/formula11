package com.formula11.service;

import com.formula11.model.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PortfolioResultado(
        Usuario usuario,
        LocalDate fechaValuacion,
        BigDecimal saldo,
        BigDecimal invertido,
        BigDecimal valorTenencias,
        BigDecimal ganancia,
        BigDecimal gananciaPorcentaje,
        List<PosicionPortfolioResultado> posiciones) {
    public PortfolioResultado {
        posiciones = List.copyOf(posiciones);
    }
}
