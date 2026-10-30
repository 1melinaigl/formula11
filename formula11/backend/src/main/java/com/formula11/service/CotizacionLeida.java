package com.formula11.service;

import com.formula11.model.Cotizacion;
import com.formula11.model.VariacionCotizacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Cotización leída para la API: precio y score con 4 decimales y variación contra la anterior. */
public record CotizacionLeida(Long jugadorId, LocalDate fecha, BigDecimal precio, BigDecimal score,
                              BigDecimal variacion, String estrategia, int version) {
    private static final int DECIMALES = 4;

    public static CotizacionLeida de(Cotizacion cotizacion, BigDecimal precioAnterior) {
        return new CotizacionLeida(
                cotizacion.getJugador().getId(),
                cotizacion.getFecha(),
                cotizacion.getPrecio().setScale(DECIMALES, RoundingMode.HALF_UP),
                cotizacion.getScore().setScale(DECIMALES, RoundingMode.HALF_UP),
                VariacionCotizacion.porcentaje(precioAnterior, cotizacion.getPrecio()),
                cotizacion.getEstrategia(),
                cotizacion.getVersion());
    }
}
