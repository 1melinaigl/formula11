package com.formula11.dto;

import java.time.Instant;
import java.util.List;

public record EjecucionMetricasResponse(Long id, String estado, Instant inicio, Instant fin,
                                         List<LigaEjecucionResponse> ligas, String correlationId) {}
