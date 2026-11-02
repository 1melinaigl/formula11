package com.formula11.unit.model.valuacion;

import com.formula11.model.valuacion.CalculadoraPrecio;
import com.formula11.model.valuacion.ParametrosPrecio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraPrecioTest {
    private static final ParametrosPrecio PARAMETROS = new ParametrosPrecio(
            new BigDecimal("0.4"), new BigDecimal("0.20"), new BigDecimal("0.10"));

    @Test
    void aplicaLaFormulaSemanalConLosValoresDeLaSpec() {
        assertPrecio("1.0000", "0.75", "1.1000");
        assertPrecio("1.0000", "1", "1.2000");
        assertPrecio("1.0000", "0", "0.8000");
        assertPrecio("0.1100", "0", "0.1000");
    }

    @Test
    void aplicaPrecioDePartidaYVariacionNullEnLaPrimeraSemana() {
        var resultado = CalculadoraPrecio.calcular(null, new BigDecimal("0.75"), PARAMETROS, true);

        assertEquals(new BigDecimal("1.1000"), resultado.precio());
        assertNull(resultado.variacion());
    }

    @Test
    void mantieneElPrecioSiNoJugoYRespetaCuatroDecimales() {
        var resultado = CalculadoraPrecio.calcular(new BigDecimal("1.23456"),
                new BigDecimal("0.75"), PARAMETROS, false);

        assertEquals(new BigDecimal("1.2346"), resultado.precio());
        assertNull(resultado.variacion());
    }

    private static void assertPrecio(String anterior, String score, String esperado) {
        var resultado = CalculadoraPrecio.calcular(new BigDecimal(anterior),
                new BigDecimal(score), PARAMETROS, true);
        assertEquals(new BigDecimal(esperado), resultado.precio());
    }
}
