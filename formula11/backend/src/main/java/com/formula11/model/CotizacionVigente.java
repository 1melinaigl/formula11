package com.formula11.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionVigente(Long jugadorId, BigDecimal precio, LocalDate fecha, String estrategia, Integer version) {}
