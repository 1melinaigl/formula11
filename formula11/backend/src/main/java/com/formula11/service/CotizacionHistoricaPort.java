package com.formula11.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CotizacionHistoricaPort {
    List<LocalDate> fechasCotizacion(LocalDate desde, LocalDate hasta);

    Map<Long, BigDecimal> preciosA(Collection<Long> jugadorIds, LocalDate fecha);
}
