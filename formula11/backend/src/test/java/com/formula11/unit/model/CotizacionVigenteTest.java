package com.formula11.unit.model;

import com.formula11.model.CotizacionVigente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNull;

class CotizacionVigenteTest {
    @Test
    void permiteFechaEstrategiaYVersionNulasParaElPrecioInicial() {
        var cotizacion = new CotizacionVigente(1L, BigDecimal.ONE, null, null, null);

        assertNull(cotizacion.fecha());
        assertNull(cotizacion.estrategia());
        assertNull(cotizacion.version());
    }
}
