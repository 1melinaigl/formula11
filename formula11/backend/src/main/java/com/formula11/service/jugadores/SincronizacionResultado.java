package com.formula11.service.jugadores;

import java.util.List;

/** Resultado de sincronizar el catálogo con el proveedor, con el detalle de cada liga. */
public record SincronizacionResultado(String estado, List<LigaSincronizada> ligas, int creados, int actualizados,
                                      int omitidos, String correlationId) {
    public record LigaSincronizada(String liga, String estado, int creados, int actualizados, int omitidos,
                                   String error) {}
}
