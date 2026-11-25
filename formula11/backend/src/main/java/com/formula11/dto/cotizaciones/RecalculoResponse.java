package com.formula11.dto.cotizaciones;

import com.formula11.service.cotizaciones.RecalculoResultado;

import java.time.*;
import java.util.List;

public record RecalculoResponse(String estado, int semanasCalculadas, int jugadoresCotizados,
                                int jugadoresSinMetricas, List<EstrategiaVersionResponse> estrategias,
                                Instant inicio, Instant fin, String correlationId) {
    public record EstrategiaVersionResponse(String codigo, int version) {}

    public static RecalculoResponse from(RecalculoResultado resultado) {
        List<EstrategiaVersionResponse> estrategias = resultado.estrategias().stream()
                .map(e -> new EstrategiaVersionResponse(e.codigo(), e.version())).toList();
        return new RecalculoResponse(resultado.estado(), resultado.semanasCalculadas(), resultado.jugadoresCotizados(),
                resultado.jugadoresSinMetricas(), estrategias, resultado.inicio(), resultado.fin(),
                resultado.correlationId());
    }
}
