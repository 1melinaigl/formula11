package com.formula11.service.metricas;

import java.time.Instant;
import java.util.List;

/** Una página de jugadores de WhoScored sin vincular. La liga va con su nombre legible. */
public record PaginaPendientes(List<Pendiente> content, int page, int size, long totalElements, int totalPages) {
    public record Pendiente(String whoscoredId, String nombre, String equipo, String liga, Instant detectadoEn) {}
}
