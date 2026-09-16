package com.formula11.unit.model;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JugadorTest {
    @Test
    void aceptaSoloLigaDelEnumSoportado() {
        Jugador jugador = new Jugador("Jugador", "Equipo", Liga.LA_LIGA, "Delantero");
        assertEquals("La Liga", jugador.getLiga().getNombreLegible());
    }

    @Test
    void rechazaLigaNula() {
        assertThrows(NullPointerException.class, () -> new Jugador("Jugador", "Equipo", null, "Defensa"));
    }
}
