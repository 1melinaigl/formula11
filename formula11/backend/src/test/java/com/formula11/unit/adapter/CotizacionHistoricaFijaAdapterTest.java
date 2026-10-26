package com.formula11.unit.adapter;

import com.formula11.adapter.CotizacionHistoricaFijaAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CotizacionHistoricaFijaAdapterTest {
    private final CotizacionHistoricaFijaAdapter adapter = new CotizacionHistoricaFijaAdapter();

    @Test
    void devuelveLunesSemanalesInclusivos() {
        assertEquals(List.of(
                LocalDate.of(2026, 10, 26),
                LocalDate.of(2026, 11, 2)),
                adapter.fechasCotizacion(
                        LocalDate.of(2026, 10, 25),
                        LocalDate.of(2026, 11, 2)));
    }

    @Test
    void devuelvePrecioFijoParaCadaJugador() {
        assertEquals(
                new BigDecimal("1.0000"),
                adapter.preciosA(List.of(3L), LocalDate.of(2026, 10, 26)).get(3L));
    }
}
