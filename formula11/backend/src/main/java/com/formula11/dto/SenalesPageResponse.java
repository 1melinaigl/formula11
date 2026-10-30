package com.formula11.dto;

import com.formula11.service.PaginaSenales;

import java.util.List;

/** Esquema {@code SenalesPageResponse}. */
public record SenalesPageResponse(List<SenalResponse> content, int page, int size, long totalElements, int totalPages) {
    public static SenalesPageResponse from(PaginaSenales pagina) {
        return new SenalesPageResponse(pagina.content().stream().map(SenalResponse::from).toList(),
                pagina.page(), pagina.size(), pagina.totalElements(), pagina.totalPages());
    }
}
