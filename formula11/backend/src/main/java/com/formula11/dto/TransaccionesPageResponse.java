package com.formula11.dto;

import com.formula11.service.HistorialTransaccionesResultado;
import java.util.List;

public record TransaccionesPageResponse(
        List<OperacionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
    public TransaccionesPageResponse {
        content = List.copyOf(content);
    }

    public static TransaccionesPageResponse from(HistorialTransaccionesResultado resultado) {
        return new TransaccionesPageResponse(
                resultado.contenido().stream()
                        .map(operacion -> OperacionResponse.from(
                                operacion.transaccion(), operacion.saldoPosterior()))
                        .toList(),
                resultado.page(),
                resultado.size(),
                resultado.totalElements(),
                resultado.totalPages());
    }
}
