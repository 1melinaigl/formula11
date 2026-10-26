package com.formula11.model.valuacion;

import java.math.BigDecimal;

public record ParametrosPrecio(BigDecimal sensibilidad, BigDecimal topeVariacion, BigDecimal piso) {
    public ParametrosPrecio {
        sensibilidad = sensibilidad == null ? new BigDecimal("0.4") : sensibilidad;
        topeVariacion = topeVariacion == null ? new BigDecimal("0.20") : topeVariacion;
        piso = piso == null ? new BigDecimal("0.10") : piso;
        if (sensibilidad.signum() < 0 || topeVariacion.signum() < 0 || piso.signum() <= 0)
            throw new IllegalArgumentException("Parámetros de precio inválidos");
    }
}
