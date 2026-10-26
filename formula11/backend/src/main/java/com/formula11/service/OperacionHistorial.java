package com.formula11.service;

import com.formula11.model.Transaccion;
import java.math.BigDecimal;

// Una operación del historial con el saldo que quedó después, leído de su auditoría.
public record OperacionHistorial(Transaccion transaccion, BigDecimal saldoPosterior) { }
