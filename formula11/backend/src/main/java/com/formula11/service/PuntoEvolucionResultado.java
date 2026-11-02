package com.formula11.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PuntoEvolucionResultado(
        LocalDate fecha,
        BigDecimal valor,
        BigDecimal invertido) {
}
