package com.formula11.service;

import com.formula11.model.Transaccion;
import java.math.BigDecimal;

// Resultado de dominio de una compra o venta: no depende de HTTP ni de DTOs.
public record ResultadoOperacion(Transaccion transaccion, BigDecimal saldoPosterior) { }
