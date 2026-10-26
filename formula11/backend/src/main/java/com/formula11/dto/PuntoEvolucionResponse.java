package com.formula11.dto;

import com.formula11.service.PuntoEvolucionResultado;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PuntoEvolucionResponse(LocalDate fecha, BigDecimal valor, BigDecimal invertido) {
    public static PuntoEvolucionResponse from(PuntoEvolucionResultado resultado) {
        return new PuntoEvolucionResponse(resultado.fecha(), resultado.valor(), resultado.invertido());
    }
}
