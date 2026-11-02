package com.formula11.model.valuacion;

import java.time.LocalDate;
import java.util.List;

public interface EstrategiaValuacion {
    ResultadoValuacion calcular(List<MetricaPartidoInput> metricas, LocalDate fecha,
                                ParametrosEstrategia parametros);
}
