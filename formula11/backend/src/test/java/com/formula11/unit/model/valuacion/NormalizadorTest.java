package com.formula11.unit.model.valuacion;

import com.formula11.model.valuacion.Normalizador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizadorTest {
    @Test
    void normalizaScoreConDesvioPoblacionalYAcota() {
        assertEquals(0.8062, Normalizador.scoreFinal(3, List.of(1d, 2d, 3d)), 0.00005);
        assertEquals(1.0, Normalizador.scoreFinal(100, List.of(1d, 2d, 3d)), 0.00001);
        assertEquals(0.0, Normalizador.scoreFinal(-100, List.of(1d, 2d, 3d)), 0.00001);
    }

    @Test
    void devuelveMedioConValoresIgualesOUnSoloJugador() {
        assertEquals(0.5, Normalizador.scoreFinal(2, List.of(2d, 2d, 2d)), 0.00001);
        assertEquals(0.5, Normalizador.scoreFinal(2, List.of(2d)), 0.00001);
        assertEquals(0.0, Normalizador.zScore(2, List.of(2d, 2d)), 0.00001);
    }
}
