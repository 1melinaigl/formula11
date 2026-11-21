package com.formula11.dto.metricas;

import com.formula11.service.metricas.ResumenEjecucion;

import java.time.Instant;
import java.util.List;

public record EjecucionMetricasResponse(Long id, String estado, Instant inicio, Instant fin,
                                         List<LigaEjecucionResponse> ligas, String correlationId) {
    public static EjecucionMetricasResponse from(ResumenEjecucion resumen) {
        List<LigaEjecucionResponse> ligas = resumen.ligas().stream()
                .map(liga -> new LigaEjecucionResponse(liga.liga(), liga.estado(), liga.partidosCargados(),
                        liga.partidosOmitidos(), liga.jugadoresVinculados(), liga.pendientes(), liga.error()))
                .toList();
        return new EjecucionMetricasResponse(resumen.id(), resumen.estado(), resumen.inicio(), resumen.fin(), ligas,
                resumen.correlationId());
    }
}
