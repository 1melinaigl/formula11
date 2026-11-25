package com.formula11.service.cotizaciones;

import java.time.Instant;
import java.util.List;

/** Resumen de un recálculo de cotizaciones, con la versión vigente de cada estrategia. */
public record RecalculoResultado(String estado, int semanasCalculadas, int jugadoresCotizados,
                                 int jugadoresSinMetricas, List<EstrategiaVersion> estrategias,
                                 Instant inicio, Instant fin, String correlationId) {
    public record EstrategiaVersion(String codigo, int version) {}
}
