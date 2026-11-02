package com.formula11.dto;

import java.time.*;
import java.util.List;

public record RecalculoResponse(String estado, int semanasCalculadas, int jugadoresCotizados,
                                int jugadoresSinMetricas, List<EstrategiaVersionResponse> estrategias,
                                Instant inicio, Instant fin, String correlationId) {
    public record EstrategiaVersionResponse(String codigo, int version) {}
}
