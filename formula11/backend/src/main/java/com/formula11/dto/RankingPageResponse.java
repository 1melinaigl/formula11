package com.formula11.dto;

import com.formula11.service.RankingResultado;

import java.time.LocalDate;
import java.util.List;

/** Esquema {@code RankingPageResponse}: datos de paginación, estrategia, versión, fecha y contenido. */
public record RankingPageResponse(int page, int size, long totalElements, int totalPages, String estrategia,
                                  Integer version, LocalDate fecha, List<RankingItemResponse> content) {
    public static RankingPageResponse from(RankingResultado resultado) {
        return new RankingPageResponse(resultado.page(), resultado.size(), resultado.totalElements(),
                resultado.totalPages(), resultado.estrategia(), resultado.version(), resultado.fecha(),
                resultado.items().stream().map(RankingItemResponse::from).toList());
    }
}
