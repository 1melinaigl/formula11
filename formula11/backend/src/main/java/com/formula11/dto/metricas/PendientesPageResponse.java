package com.formula11.dto.metricas;

import java.util.List;

public record PendientesPageResponse(List<PendienteVinculacion> content, int page, int size,
                                     long totalElements, int totalPages) {}
