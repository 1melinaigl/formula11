package com.formula11.dto.jugadores;

import com.formula11.service.jugadores.SincronizacionResultado;

import java.util.List;

public record SincronizacionResponse(
        String estado,
        List<LigaSyncResultResponse> ligas,
        int creados,
        int actualizados,
        int omitidos,
        String correlationId) {
    public static SincronizacionResponse from(SincronizacionResultado resultado) {
        List<LigaSyncResultResponse> ligas = resultado.ligas().stream()
                .map(liga -> new LigaSyncResultResponse(liga.liga(), liga.estado(), liga.creados(), liga.actualizados(),
                        liga.omitidos(), liga.error()))
                .toList();
        return new SincronizacionResponse(resultado.estado(), ligas, resultado.creados(), resultado.actualizados(),
                resultado.omitidos(), resultado.correlationId());
    }
}
