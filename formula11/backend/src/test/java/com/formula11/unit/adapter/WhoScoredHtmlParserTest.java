package com.formula11.unit.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.WhoScoredHtmlParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhoScoredHtmlParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Mismo formato que una página real guardada con Ctrl+U (claves sin comillas dentro de args). */
    @Test
    void extraeMatchIdYLosDosObjetosDelFormatoReal() throws Exception {
        String html = """
                <script>
                        require.config.params["args"] = {
                            matchId:1994169,
                            matchCentreData: {"startTime":"2026-10-09T20:00:00","home":{"name":"Malaga"},
                              "message":"texto con } y {"},
                            matchCentreEventTypeJson: {"assist":92,"yellowCard":65},
                            formationIdNameMappings: {"1":"442"}
                        };
                </script>
                """;

        var datos = WhoScoredHtmlParser.extraer(html);

        assertEquals(1994169L, datos.matchId());
        assertEquals("Malaga", objectMapper.readTree(datos.matchCentreData()).path("home").path("name").asText());
        assertTrue(datos.matchCentreData().contains("\"message\""));
        assertEquals(objectMapper.readTree("{\"assist\":92,\"yellowCard\":65}"),
                objectMapper.readTree(datos.matchCentreEventTypeJson()));
    }

    @Test
    void aceptaAsignacionesConIgualYDevuelveMatchIdNegativoSiFalta() {
        String html = """
                require.config.params.args.matchCentreData = {"startTime":"2026-10-09T20:00:00"};
                require.config.params.args.matchCentreEventTypeJson = {"assist":92};
                """;

        var datos = WhoScoredHtmlParser.extraer(html);

        assertEquals(-1L, datos.matchId());
        assertTrue(datos.matchCentreData().contains("startTime"));
    }

    @Test
    void fallaExplicitamenteCuandoFaltaUnObjeto() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WhoScoredHtmlParser.extraer("<html>sin datos</html>"));

        assertTrue(error.getMessage().contains("matchCentreData"));
    }

    @Test
    void fallaCuandoElObjetoEstaIncompleto() {
        String html = "matchCentreData: {\"incompleto\": true";

        assertThrows(IllegalArgumentException.class, () -> WhoScoredHtmlParser.extraer(html));
    }

    @Test
    void fallaCuandoElSegundoObjetoEsInvalido() {
        String html = """
                matchCentreData: {},
                matchCentreEventTypeJson: { "texto": },
                """;

        assertThrows(IllegalArgumentException.class, () -> WhoScoredHtmlParser.extraer(html));
    }
}
