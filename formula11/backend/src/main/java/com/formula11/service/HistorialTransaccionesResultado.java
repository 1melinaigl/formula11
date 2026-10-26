package com.formula11.service;

import java.util.List;

public record HistorialTransaccionesResultado(
        List<OperacionHistorial> contenido,
        int page,
        int size,
        long totalElements,
        int totalPages) {
    public HistorialTransaccionesResultado {
        contenido = List.copyOf(contenido);
    }
}
