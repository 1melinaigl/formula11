package com.formula11.unit.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.CalendarioWhoScoredParser;
import com.formula11.model.Liga;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class CalendarioWhoScoredParserTest {
    private static final ZoneId LONDRES = ZoneId.of("Europe/London");
    private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    private final CalendarioWhoScoredParser parser = new CalendarioWhoScoredParser(new ObjectMapper());

    /** Formato real de /tournaments/25544/data/?d=202610&isAggregate=false (recortado). */
    @Test
    void parseaElFormatoRealDelCalendario() {
        String json = """
                {"createdAt":"2026-10-27T20:17:42Z","tournaments":[{"tournamentId":2,"stageId":25544,
                  "tournamentName":"Premier League","seasonName":"2026/2027","matches":[
                    {"stageId":25544,"id":1983591,"status":6,"startTime":"2026-10-10T12:30:00",
                     "homeTeamId":13,"homeTeamName":"Arsenal","awayTeamId":19,"awayTeamName":"Leeds",
                     "elapsed":"FT","isTopMatch":true},
                    {"stageId":25544,"id":1983600,"status":1,"startTime":"2026-11-01T15:00:00",
                     "homeTeamId":30,"homeTeamName":"Tottenham","awayTeamId":32,"awayTeamName":"Chelsea",
                     "elapsed":"-"}
                ]}]}
                """;

        var partidos = parser.parse(json, Liga.PREMIER_LEAGUE, LONDRES, BUENOS_AIRES);

        assertEquals(2, partidos.size());
        var terminado = partidos.get(0);
        assertEquals(1983591L, terminado.idExterno());
        assertEquals("Arsenal", terminado.equipoLocal());
        assertEquals("Leeds", terminado.equipoVisitante());
        assertEquals("FT", terminado.elapsed());
        assertEquals(Liga.PREMIER_LEAGUE, terminado.liga());
        // 12:30 en Londres (horario de verano, UTC+1) son las 08:30 en Buenos Aires.
        assertEquals(LocalDateTime.of(2026, 10, 10, 8, 30), terminado.startTime().toLocalDateTime());
        assertEquals("-", partidos.get(1).elapsed());
    }

    @Test
    void devuelveListaVaciaSiNoHayPartidos() {
        assertTrue(parser.parse("{\"tournaments\":[]}", Liga.SERIE_A, LONDRES, BUENOS_AIRES).isEmpty());
    }

    @Test
    void rechazaPartidoSinFecha() {
        String json = """
                {"tournaments":[{"matches":[
                  {"id":104,"homeTeamName":"Local","awayTeamName":"Visitante","elapsed":"FT"}
                ]}]}
                """;

        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(json, Liga.SERIE_A, LONDRES, BUENOS_AIRES));
    }

    @Test
    void rechazaPartidoSinEquipo() {
        String json = """
                {"tournaments":[{"matches":[
                  {"id":105,"startTime":"2026-10-10T12:30:00","homeTeamName":"Local","elapsed":"FT"}
                ]}]}
                """;

        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(json, Liga.SERIE_A, LONDRES, BUENOS_AIRES));
    }

    @Test
    void rechazaPartidoSinId() {
        String json = """
                {"tournaments":[{"matches":[
                  {"startTime":"2026-10-10T12:30:00","homeTeamName":"A","awayTeamName":"B","elapsed":"FT"}
                ]}]}
                """;

        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(json, Liga.SERIE_A, LONDRES, BUENOS_AIRES));
    }

    @Test
    void rechazaJsonInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("{", Liga.SERIE_A, LONDRES, BUENOS_AIRES));
    }
}
