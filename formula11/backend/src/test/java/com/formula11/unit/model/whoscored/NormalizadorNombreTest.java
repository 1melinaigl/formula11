package com.formula11.unit.model.whoscored;

import com.formula11.model.excepciones.DatoInvalidoException;

import com.formula11.model.whoscored.NormalizadorNombre;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizadorNombreTest {
    @Test
    void normalizaDiacriticosMayusculasYEspacios() {
        assertEquals("angel fortuno", NormalizadorNombre.normalizar("  Ángel   FORTUÑO  "));
    }

    @Test
    void rechazaEntradaNulaYBlanca() {
        assertThrows(DatoInvalidoException.class, () -> NormalizadorNombre.normalizar(null));
        assertThrows(DatoInvalidoException.class, () -> NormalizadorNombre.normalizar(" \t "));
    }
}
