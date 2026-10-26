package com.formula11.unit.model;

import com.formula11.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MercadoModelTest {
    private final Usuario usuario = new Usuario("Ana", "ana-model@example.com", "hash");
    private final Jugador jugador = new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero");

    @Test
    void cuentaDebitaYAcreditaConCuatroDecimales() {
        Cuenta cuenta = new Cuenta(usuario, new BigDecimal("100"));
        cuenta.debitar(new BigDecimal("1.23456"));
        cuenta.acreditar(new BigDecimal("0.23456"));
        assertEquals(new BigDecimal("99.0000"), cuenta.getSaldo());
    }

    @Test
    void tenenciaCalculaPromedioPonderado() {
        Tenencia tenencia = new Tenencia(usuario, jugador, 2, new BigDecimal("1.0000"));
        tenencia.comprar(2, new BigDecimal("2.0000"));
        assertEquals(4, tenencia.getCantidad());
        assertEquals(new BigDecimal("1.5000"), tenencia.getPrecioPromedio());
    }

    @Test
    void rechazaEstadosNoPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new Cuenta(usuario, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> new Tenencia(usuario, jugador, -1, BigDecimal.ONE));
        Tenencia tenencia = new Tenencia(usuario, jugador, 1, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> tenencia.comprar(0, BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> tenencia.vender(2));
    }
}
