package com.formula11.adapter;

import com.formula11.model.Liga;

import java.time.YearMonth;
import java.util.List;

public interface WhoScoredPort {
    List<PartidoWhoScoredResumen> listarPartidos(Liga liga, YearMonth mes);

    DatosPartidoWhoScored obtenerDatos(long partidoId);

    /**
     * Libera los recursos de una carga (por ejemplo, el navegador). Se llama al terminar cada carga,
     * haya salido bien o mal. Por defecto no hace nada.
     */
    default void finalizarCarga() {
        // Las implementaciones sin recursos externos no necesitan liberar nada.
    }
}
