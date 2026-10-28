package com.formula11.adapter;

import com.formula11.model.Liga;

import java.time.YearMonth;
import java.util.List;

public interface WhoScoredPort {
    List<PartidoWhoScoredResumen> listarPartidos(Liga liga, YearMonth mes);
    DatosPartidoWhoScored obtenerDatos(long partidoId);
}
