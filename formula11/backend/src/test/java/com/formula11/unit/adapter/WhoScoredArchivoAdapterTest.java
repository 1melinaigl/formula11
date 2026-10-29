package com.formula11.unit.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.WhoScoredArchivoAdapter;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.Liga;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class WhoScoredArchivoAdapterTest {
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private static WhoScoredArchivoAdapter adapter(String carpeta) {
        WhoScoredProperties properties = new WhoScoredProperties();
        properties.setCarpetaPaginas(carpeta);
        return new WhoScoredArchivoAdapter(properties, new ObjectMapper());
    }

    @Test
    void listaElFixtureEnSuLigaYMes() {
        WhoScoredArchivoAdapter adapter = adapter("src/test/resources/whoscored");

        var partidos = adapter.listarPartidos(Liga.LA_LIGA, OCTUBRE);

        assertEquals(1, partidos.size());
        var partido = partidos.get(0);
        assertEquals(1994169L, partido.idExterno());
        assertEquals("FT", partido.elapsed());
        assertEquals("Malaga", partido.equipoLocal());
        assertEquals("Espanyol", partido.equipoVisitante());
        assertEquals(Liga.LA_LIGA, partido.liga());
        assertEquals("2026-10-09", partido.startTime().toLocalDate().toString());
    }

    @Test
    void noListaElFixtureEnOtraLigaNiEnOtroMes() {
        WhoScoredArchivoAdapter adapter = adapter("src/test/resources/whoscored");

        assertTrue(adapter.listarPartidos(Liga.SERIE_A, OCTUBRE).isEmpty());
        assertTrue(adapter.listarPartidos(Liga.LA_LIGA, YearMonth.of(2026, 9)).isEmpty());
    }

    @Test
    void obtieneLosDatosDelFixturePorId() {
        WhoScoredArchivoAdapter adapter = adapter("src/test/resources/whoscored");

        var datos = adapter.obtenerDatos(1994169L);

        assertEquals(1994169L, datos.idExterno());
        assertEquals(Liga.LA_LIGA, datos.liga());
        assertEquals("2026-10-09", datos.startTime().toLocalDate().toString());
        assertTrue(datos.matchCentreData().contains("\"startTime\""));
        assertTrue(datos.matchCentreEventTypeJson().contains("assist"));
    }

    @Test
    void rechazaPartidoInexistente(@TempDir Path carpeta) {
        WhoScoredArchivoAdapter adapter = adapter(carpeta.toString());

        assertThrows(IllegalArgumentException.class, () -> adapter.obtenerDatos(1));
    }

    @Test
    void leePaginasHtmlEIgnoraArchivosInvalidos(@TempDir Path carpeta) throws Exception {
        Path serieA = Files.createDirectories(carpeta.resolve(Liga.SERIE_A.name()));
        Files.writeString(serieA.resolve("partido.html"), """
                require.config.params["args"] = {
                    matchId:555,
                    matchCentreData: {"startTime":"2026-10-04T14:00:00","elapsed":"FT",
                      "home":{"name":"Inter"},"away":{"name":"Milan"}},
                    matchCentreEventTypeJson: {"assist":92}
                };
                """);
        Files.writeString(serieA.resolve("roto.json"), "{ no es json");
        WhoScoredArchivoAdapter adapter = adapter(carpeta.toString());

        var partidos = adapter.listarPartidos(Liga.SERIE_A, OCTUBRE);

        assertEquals(1, partidos.size());
        assertEquals(555L, partidos.get(0).idExterno());
        assertEquals("Inter", partidos.get(0).equipoLocal());
        assertEquals(Liga.SERIE_A, adapter.obtenerDatos(555L).liga());
    }

    @Test
    void finalizarCargaReleeLaCarpeta(@TempDir Path carpeta) throws Exception {
        Path ligue1 = Files.createDirectories(carpeta.resolve(Liga.LIGUE_1.name()));
        WhoScoredArchivoAdapter adapter = adapter(carpeta.toString());
        assertTrue(adapter.listarPartidos(Liga.LIGUE_1, OCTUBRE).isEmpty());

        Files.writeString(ligue1.resolve("partido.json"), """
                {"matchId":777,
                 "matchCentreData":{"startTime":"2026-10-18T19:00:00","elapsed":"FT",
                   "home":{"name":"PSG"},"away":{"name":"Lyon"}},
                 "matchCentreEventTypeJson":{"assist":92}}
                """);
        adapter.finalizarCarga();

        assertEquals(1, adapter.listarPartidos(Liga.LIGUE_1, OCTUBRE).size());
    }
}
