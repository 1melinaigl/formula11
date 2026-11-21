package com.formula11.dto.metricas;

import com.formula11.service.metricas.PaginaPendientes;

import java.util.List;

public record PendientesPageResponse(List<PendienteVinculacion> content, int page, int size,
                                     long totalElements, int totalPages) {
    public static PendientesPageResponse from(PaginaPendientes pagina) {
        List<PendienteVinculacion> content = pagina.content().stream()
                .map(p -> new PendienteVinculacion(p.whoscoredId(), p.nombre(), p.equipo(), p.liga(), p.detectadoEn()))
                .toList();
        return new PendientesPageResponse(content, pagina.page(), pagina.size(), pagina.totalElements(),
                pagina.totalPages());
    }
}
