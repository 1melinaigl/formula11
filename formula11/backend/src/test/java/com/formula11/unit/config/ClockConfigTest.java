package com.formula11.unit.config;

import com.formula11.config.ClockConfig;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockConfigTest {
    @Test
    void creaClockEnUtcConFechaFijaYPermiteObtenerFechaDeNegocio() {
        Clock clock = new ClockConfig().clock("2026-10-24");

        assertEquals(ZoneOffset.UTC, clock.getZone());
        assertEquals(LocalDate.of(2026, 10, 24),
                LocalDate.ofInstant(clock.instant(), ClockConfig.ZONA_NEGOCIO));
    }

    @Test
    void creaClockDeSistemaCuandoNoHayFechaFija() {
        Clock clock = new ClockConfig().clock("");

        assertEquals(ZoneOffset.UTC, clock.getZone());
    }
}
