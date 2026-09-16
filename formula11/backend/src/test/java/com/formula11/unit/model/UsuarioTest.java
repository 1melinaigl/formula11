package com.formula11.unit.model;

import com.formula11.model.Usuario;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {
    @Test
    void normalizaEmailYConservaNombreSaneado() {
        Usuario usuario = new Usuario(" Ana ", " ANA@EXAMPLE.COM ", "hash");
        assertEquals("Ana", usuario.getNombre());
        assertEquals("ana@example.com", usuario.getEmail());
    }

    @Test
    void rechazaEmailInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Usuario("Ana", "sin-email", "hash"));
    }
}
