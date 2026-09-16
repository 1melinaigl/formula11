package com.formula11.unit.security;

import com.formula11.security.JwtService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService("test-secret-key-that-is-at-least-32-bytes-long", 3600);

    @Test
    void generaYValidaToken() {
        String token = jwtService.generarToken(1L, "ana@example.com");
        assertTrue(jwtService.esValido(token));
        assertEquals("ana@example.com", jwtService.extraerEmail(token));
    }

    @Test
    void rechazaTokenManipulado() {
        assertFalse(jwtService.esValido("token.invalido.firmado"));
    }
}
