package com.formula11.dto;

import com.formula11.service.MercadoJugadorResultado;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MercadoJugadorResponse(
        Long jugadorId,
        int tokensEmitidos,
        int tokensDisponibles,
        BigDecimal precioVigente,
        LocalDate fechaCotizacion
) {
    public static MercadoJugadorResponse from(MercadoJugadorResultado resultado) {
        return new MercadoJugadorResponse(
                resultado.jugadorId(),
                resultado.tokensEmitidos(),
                resultado.tokensDisponibles(),
                resultado.precioVigente().setScale(4),
                resultado.fechaCotizacion());
    }
}
