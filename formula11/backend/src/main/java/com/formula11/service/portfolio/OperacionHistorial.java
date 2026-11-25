package com.formula11.service.portfolio;

import com.formula11.model.mercado.Transaccion;
import java.math.BigDecimal;


public record OperacionHistorial(Transaccion transaccion, BigDecimal saldoPosterior) { }
