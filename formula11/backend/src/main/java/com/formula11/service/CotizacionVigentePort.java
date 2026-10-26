package com.formula11.service;

import com.formula11.model.Jugador;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public interface CotizacionVigentePort {
    CotizacionVigente vigente(Jugador jugador, Instant fecha);

    record CotizacionVigente(BigDecimal precio, String estrategia, Integer version, LocalDate fechaCotizacion) {
        public CotizacionVigente {
            if (precio == null || precio.signum() <= 0) {
                throw new IllegalArgumentException("La cotización debe ser positiva");
            }
            precio = precio.setScale(4, java.math.RoundingMode.HALF_UP);
        }
    }
}
