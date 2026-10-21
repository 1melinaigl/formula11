package com.formula11.dto;

import java.util.List;

public record JugadoresPageResponse(
        List<JugadorResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
