package com.formula11.unit.model;

import com.formula11.model.VariacionCotizacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VariacionCotizacionTest {

    @Test
    void laPrimeraCotizacionNoTieneVariacion() {
        assertNull(VariacionCotizacion.porcentaje(null, new BigDecimal("1.1000")));
    }

    @Test
    void calculaUnaSuba() {
        assertEquals(new BigDecimal("10.00"),
                VariacionCotizacion.porcentaje(new BigDecimal("1.0000"), new BigDecimal("1.1000")));
    }

    @Test
    void calculaUnaBajaConSigno() {
        assertEquals(new BigDecimal("-20.00"),
                VariacionCotizacion.porcentaje(new BigDecimal("1.2000"), new BigDecimal("0.9600")));
    }

    @Test
    void sinCambioDaCero() {
        assertEquals(new BigDecimal("0.00"),
                VariacionCotizacion.porcentaje(new BigDecimal("1.5000"), new BigDecimal("1.5000")));
    }

    @Test
    void redondeaHalfUpADosDecimales() {
        // (2.0001 / 2 - 1) * 100 = 0.005 exacto: HALF_UP sube a 0.01.
        assertEquals(new BigDecimal("0.01"),
                VariacionCotizacion.porcentaje(new BigDecimal("2.0000"), new BigDecimal("2.0001")));
        // (1 / 3 - 1) * 100 = -66.666...
        assertEquals(new BigDecimal("-66.67"),
                VariacionCotizacion.porcentaje(new BigDecimal("3.0000"), new BigDecimal("1.0000")));
    }

    @Test
    void rechazaPrecioAnteriorNoPositivo() {
        BigDecimal actual = new BigDecimal("1.0000");
        BigDecimal cero = BigDecimal.ZERO;

        assertThrows(IllegalArgumentException.class, () -> VariacionCotizacion.porcentaje(cero, actual));
    }

    @Test
    void rechazaPrecioActualNulo() {
        BigDecimal anterior = new BigDecimal("1.0000");

        assertThrows(IllegalArgumentException.class, () -> VariacionCotizacion.porcentaje(anterior, null));
    }
}
