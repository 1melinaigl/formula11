package com.formula11.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MercadoJugadorResultado(
        Long jugadorId,
        int tokensEmitidos,
        int tokensDisponibles,
        BigDecimal precioVigente,
        LocalDate fechaCotizacion
) { }
