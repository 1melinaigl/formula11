package com.formula11.dto;

/** Resumen por liga de una carga de métricas. La liga va con su nombre legible, como exige el contrato. */
public record LigaEjecucionResponse(String liga, String estado, int partidosCargados,
                                    int partidosOmitidos, int jugadoresVinculados,
                                    int pendientes, String error) {}
