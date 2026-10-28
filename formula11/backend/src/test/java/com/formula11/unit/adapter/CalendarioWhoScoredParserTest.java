package com.formula11.unit.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.CalendarioWhoScoredParser;
import com.formula11.model.Liga;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CalendarioWhoScoredParserTest {
    @Test
    void parseaPartidoTerminadoYNoTerminadoYConvierteZona() {
        String json = """
                {"tournaments":[{"matches":[
                  {"id":101,"startTime":"2026-10-09T20:00:00","homeTeam":{"name":"Málaga"},
                   "awayTeam":{"name":"Espanyol"},"status":{"name":"FT","type":"finished"}},
                  {"id":102,"startTime":"2026-10-10T20:00:00","homeTeam":{"name":"A"},
                   "awayTeam":{"name":"B"},"status":{"name":"NS","type":"scheduled"}}
                ]}]}
                """;

        var partidos = new CalendarioWhoScoredParser(new ObjectMapper())
                .parse(json, Liga.LA_LIGA, ZoneId.of("Europe/London"), ZoneOffset.of("-03:00"));

        assertEquals(2, partidos.size());
        assertEquals(101L, partidos.get(0).idExterno());
        assertEquals("FT", partidos.get(0).elapsed());
        assertEquals("Málaga", partidos.get(0).equipoLocal());
        assertEquals("Espanyol", partidos.get(0).equipoVisitante());
        assertEquals("2026-10-09T16:00-03:00", partidos.get(0).startTime().toString());
        assertEquals("NS", partidos.get(1).elapsed());
    }

    @Test
    void aceptaStatusTextualYEquiposComoTexto() {
        String json = """
                {"tournaments":[{"matches":[
                  {"id":103,"startTime":"2026-10-09T20:00:00",
                   "homeTeam":"Local","awayTeam":"Visitante","status":"FT"}
                ]}]}
                """;

        var partidos = new CalendarioWhoScoredParser(new ObjectMapper())
                .parse(json, Liga.SERIE_A, ZoneId.of("UTC"), ZoneId.of("UTC"));

        assertEquals("Local", partidos.get(0).equipoLocal());
        assertEquals("Visitante", partidos.get(0).equipoVisitante());
        assertEquals("FT", partidos.get(0).elapsed());
    }

    @Test
    void rechazaPartidoSinFecha() {
        String json = """
                {"tournaments":[{"matches":[
                  {"id":104,"homeTeam":{"name":"Local"},"awayTeam":{"name":"Visitante"}}
                ]}]}
                """;

        assertThrows(IllegalArgumentException.class, () ->
                new CalendarioWhoScoredParser(new ObjectMapper()).parse(
                        json, Liga.SERIE_A, ZoneId.of("UTC"), ZoneId.of("UTC")));
    }

    @Test
    void rechazaJsonInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                new CalendarioWhoScoredParser(new ObjectMapper()).parse(
                        "{", Liga.SERIE_A, ZoneId.of("UTC"), ZoneId.of("UTC")));
    }
}
