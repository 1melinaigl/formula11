package com.formula11.service.metricas;

import java.time.Instant;
import java.util.List;

/** Estado de una carga de métricas, con el resumen de cada liga. La liga va con su nombre legible. */
public record ResumenEjecucion(Long id, String estado, Instant inicio, Instant fin, List<Liga> ligas,
                               String correlationId) {
    public record Liga(String liga, String estado, int partidosCargados, int partidosOmitidos,
                       int jugadoresVinculados, int pendientes, String error) {}
}
