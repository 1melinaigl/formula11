package com.formula11.unit.adapter;

import com.formula11.adapter.WhoScoredHtmlParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WhoScoredHtmlParserTest {
    @Test
    void extraeLosDosObjetosAunqueElScriptTengaTextoAnidado() throws Exception {
        String html = """
                <script>
                require.config.params.args.matchCentreData = {
                  "startTime":"2026-10-09T20:00:00",
                  "home":{"name":"Málaga","players":[]},
                  "message":"texto con } y {"
                };
                require.config.params.args.matchCentreEventTypeJson = {
                  "assist":92,"yellowCard":65
                };
                </script>
                """;

        var datos = WhoScoredHtmlParser.extraer(html);

        assertTrue(datos.matchCentreData().contains("\"startTime\""));
        assertTrue(datos.matchCentreData().contains("\"message\""));
        assertEquals(new ObjectMapper().readTree("{\"assist\":92,\"yellowCard\":65}"),
                new ObjectMapper().readTree(datos.matchCentreEventTypeJson()));
    }

    @Test
    void fallaExplicitamenteCuandoFaltaUnObjeto() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> WhoScoredHtmlParser.extraer("<html>sin datos</html>"));

        assertTrue(error.getMessage().contains("matchCentreData"));
    }

    @Test
    void fallaCuandoElObjetoEstaIncompleto() {
        assertThrows(IllegalArgumentException.class, () ->
                WhoScoredHtmlParser.extraer(
                        "require.config.params.args.matchCentreData = {\"incompleto\": true"));
    }

    @Test
    void fallaCuandoElSegundoObjetoEsInvalido() {
        String html = """
                require.config.params.args.matchCentreData = {};
                require.config.params.args.matchCentreEventTypeJson = { "texto": };
                """;

        assertThrows(IllegalArgumentException.class, () -> WhoScoredHtmlParser.extraer(html));
    }
}
