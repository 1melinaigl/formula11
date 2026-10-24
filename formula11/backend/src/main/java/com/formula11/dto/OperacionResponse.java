package com.formula11.dto;

import com.formula11.model.Transaccion;
import com.formula11.service.ResultadoOperacion;
import java.math.BigDecimal;
import java.time.Instant;

public record OperacionResponse(
        Long id,
        String tipo,
        JugadorResponse jugador,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal total,
        BigDecimal saldoPosterior,
        String contraparte,
        Instant fecha,
        String estrategia,
        Integer version
) {
    public static OperacionResponse from(ResultadoOperacion resultado) {
        Transaccion transaccion = resultado.transaccion();
        return new OperacionResponse(
                transaccion.getId(),
                transaccion.getTipo().name(),
                JugadorResponse.from(transaccion.getJugador()),
                transaccion.getCantidad(),
                transaccion.getPrecioUnitario(),
                transaccion.getTotal(),
                resultado.saldoPosterior().setScale(4, java.math.RoundingMode.HALF_UP),
                "superusuario",
                transaccion.getFecha(),
                transaccion.getEstrategia(),
                transaccion.getVersionCotizacion());
    }
}
