package com.formula11.service.cotizaciones;

import java.time.LocalDate;

/** Rango de semanas a recalcular. Desde y hasta van juntas, o las dos vacías para la última semana cerrada. */
public record RangoRecalculo(LocalDate desde, LocalDate hasta) {
    public boolean esValido() {
        return (desde == null && hasta == null) || (desde != null && hasta != null && !desde.isAfter(hasta));
    }
}
