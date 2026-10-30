package com.formula11.service;

import java.util.List;

/** Página de señales. */
public record PaginaSenales(List<SenalLeida> content, int page, int size, long totalElements, int totalPages) {
    public PaginaSenales {
        content = List.copyOf(content);
    }
}
