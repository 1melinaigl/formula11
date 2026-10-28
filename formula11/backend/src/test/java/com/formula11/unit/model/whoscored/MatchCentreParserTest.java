package com.formula11.unit.model.whoscored;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.model.whoscored.MatchCentreParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MatchCentreParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MatchCentreParser parser;

    @BeforeEach
    void setUp() {
        parser = new MatchCentreParser(objectMapper);
    }

    @Test
    void parseaJugadoresEventosYReglasDelFixture() throws Exception {
        JsonNode root = objectMapper.readTree(fixture());

        MatchCentreParser.Partido partido = parser.parse(
                root.get("matchCentreData").toString(),
                root.get("matchCentreEventTypeJson").toString());

        Map<Long, MatchCentreParser.Jugador> jugadores = partido.jugadores().stream()
                .collect(Collectors.toMap(MatchCentreParser.Jugador::playerId, Function.identity()));

        assertJugador(jugadores.get(261509L), "Alfonso Herrero", true, 96, "6.47");
        assertJugador(jugadores.get(447938L), "Carlos Puga", true, 63, "6.80");
        assertJugador(jugadores.get(449409L), "Ángel Fortuño", true, 96, "7.24");
        assertJugador(jugadores.get(429273L), "Bryan Zaragoza", true, 74, null);
        assertEquals(1, jugadores.get(429273L).goles());
        assertEquals(1, jugadores.get(429273L).amarillas());
        assertEquals(1, jugadores.get(429273L).tiros());
        assertEquals(1, jugadores.get(429273L).pasesClave());
        assertEquals(4, jugadores.get(429273L).regates());
        assertJugador(jugadores.get(504195L), "Adrián Niño", false, 33, null);
        assertEquals(1, jugadores.get(504195L).goles());
        assertEquals(2, jugadores.get(504195L).tiros());
        assertEquals(1, jugadores.get(506010L).asistencias());
        assertEquals(1, jugadores.get(506010L).amarillas());
        assertEquals(1, jugadores.get(508349L).asistencias());
        assertEquals(1, jugadores.get(447938L).amarillas());
        assertEquals(1, jugadores.get(447938L).pasesClave());
        assertEquals(1, jugadores.get(447938L).regates());
        assertEquals(2, jugadores.get(447938L).entradas());
        assertJugador(jugadores.get(448303L), "David Larrubia", true, 96, null);
        assertEquals(2, jugadores.get(448303L).tiros());
        assertEquals(1, jugadores.get(448303L).pasesClave());
        assertEquals(7, jugadores.get(448303L).regates());
        assertEquals(2, jugadores.get(261509L).atajadas());
        assertEquals(1, jugadores.get(261509L).golesRecibidos());
        assertEquals(0, jugadores.get(261509L).vallaInvicta());
        assertEquals(6, jugadores.get(449409L).atajadas());
        assertEquals(1, jugadores.get(449409L).golesRecibidos());
        assertEquals(0, jugadores.get(449409L).vallaInvicta());
        assertEquals(32, jugadores.size());
        assertEquals(2, partido.goles());
        assertEquals(2, partido.asistencias());
        assertEquals(9, partido.amarillas());
        assertEquals("Bryan Zaragoza", jugadores.get(429273L).nombre());
        assertEquals("Adrián Niño", jugadores.get(504195L).nombre());
        assertEquals("Javi Hernández", jugadores.get(506010L).nombre());
        assertEquals("Rafita", jugadores.get(508349L).nombre());
        assertFalse(jugadores.containsKey(531460L));
    }

    @Test
    void omiteSuplentesConStatsVacios() throws Exception {
        JsonNode root = objectMapper.readTree(fixture());
        MatchCentreParser.Partido partido = parser.parse(
                root.get("matchCentreData").toString(),
                root.get("matchCentreEventTypeJson").toString());

        assertFalse(partido.jugadores().stream().anyMatch(jugador ->
                jugador.nombre().equals("Rafel Bauzà") || jugador.nombre().equals("Ramón Enríquez")));
    }

    private static void assertJugador(MatchCentreParser.Jugador jugador, String nombre,
                                      boolean titular, int minutos, String rating) {
        assertNotNull(jugador);
        assertEquals(nombre, jugador.nombre());
        assertEquals(titular, jugador.titular());
        assertEquals(minutos, jugador.minutos());
        if (rating != null) {
            assertEquals(new BigDecimal(rating), jugador.rating());
        }
    }

    @Test
    void calculaVallaInvictaSoloConSesentaMinutosOMas() {
        String data = """
                {
                  "expandedMaxMinute": 90,
                  "events": [],
                  "home": {
                    "teamId": 1, "name": "Local",
                    "players": [
                      {"playerId": 1, "name": "Arquero Noventa", "position": "GK",
                       "isFirstEleven": true, "stats": {"ratings": {"0": 6.0}}}
                    ]
                  },
                  "away": {
                    "teamId": 2, "name": "Visitante",
                    "players": [
                      {"playerId": 2, "name": "Arquero Cuarenta y Cinco", "position": "GK",
                       "isFirstEleven": false, "subbedInExpandedMinute": 0,
                       "subbedOutExpandedMinute": 45, "stats": {"ratings": {"0": 6.0}}}
                    ]
                  }
                }
                """;

        MatchCentreParser.Partido partido = parser.parse(data, "{\"assist\": 92}");
        Map<Long, MatchCentreParser.Jugador> jugadores = partido.jugadores().stream()
                .collect(Collectors.toMap(MatchCentreParser.Jugador::playerId, Function.identity()));

        assertEquals(90, jugadores.get(1L).minutos());
        assertEquals(1, jugadores.get(1L).vallaInvicta());
        assertEquals(45, jugadores.get(2L).minutos());
        assertEquals(0, jugadores.get(2L).vallaInvicta());
    }

    private static InputStream fixture() {
        InputStream stream = MatchCentreParserTest.class.getResourceAsStream(
                "/whoscored/LA_LIGA/partido-1994169.json");
        assertNotNull(stream, "No se encontró el fixture de WhoScored");
        return stream;
    }
}
