package com.formula11.dto;

import com.formula11.model.Liga;

public record LigaEjecucionResponse(Liga liga, String estado, int partidosCargados,
                                    int partidosOmitidos, int jugadoresVinculados,
                                    int pendientes, String error) {}
