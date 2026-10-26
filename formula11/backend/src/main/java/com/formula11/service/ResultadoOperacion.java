package com.formula11.service;

import com.formula11.model.Transaccion;
import java.math.BigDecimal;


public record ResultadoOperacion(Transaccion transaccion, BigDecimal saldoPosterior) { }
