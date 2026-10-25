package com.formula11.service;

import com.formula11.model.CotizacionVigente;
import java.time.LocalDate;

public interface CotizacionVigentePort {
    CotizacionVigente obtener(Long jugadorId, LocalDate fecha);
}
