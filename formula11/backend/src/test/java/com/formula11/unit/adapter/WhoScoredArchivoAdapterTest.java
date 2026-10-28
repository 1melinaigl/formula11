package com.formula11.unit.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.WhoScoredArchivoAdapter;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.Liga;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhoScoredArchivoAdapterTest {
    @Test
    void leeElFixtureJsonDesdeLaSubcarpetaDeLaLiga() {
        WhoScoredProperties properties = new WhoScoredProperties();
        properties.setCarpetaPaginas("src/test/resources/whoscored");
        WhoScoredArchivoAdapter adapter = new WhoScoredArchivoAdapter(properties, new ObjectMapper());

        var datos = adapter.obtenerDatos(1994169L);

        assertEquals(1994169L, datos.idExterno());
        assertEquals(Liga.LA_LIGA, datos.liga());
        assertEquals("2026-10-09", datos.startTime().toLocalDate().toString());
        assertTrue(datos.matchCentreData().contains("\"startTime\""));
    }
}
