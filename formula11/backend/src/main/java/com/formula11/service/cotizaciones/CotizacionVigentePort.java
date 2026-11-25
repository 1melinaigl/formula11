package com.formula11.service.cotizaciones;

import com.formula11.model.jugadores.Jugador;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.math.RoundingMode;

public interface CotizacionVigentePort {
    CotizacionVigente vigente(Jugador jugador, Instant fecha);

    record CotizacionVigente(BigDecimal precio, String estrategia, Integer version, LocalDate fechaCotizacion) {
        public CotizacionVigente {
            if (precio == null || precio.signum() <= 0) {
                throw new IllegalArgumentException("La cotización debe ser positiva");
            }
            precio = precio.setScale(4, RoundingMode.HALF_UP);
        }
    }
}
