package com.formula11.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.DatosPartidoWhoScored;
import com.formula11.adapter.PartidoWhoScoredResumen;
import com.formula11.config.ClockConfig;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.MetricaPartido;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.MetricaPartidoRepository;
import com.formula11.service.CargaPartidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Carga el partido real del fixture (Málaga 1-1 Espanyol) contra PostgreSQL y verifica el guardado
 * transaccional, la vinculación y la idempotencia. Usa un id externo y una fecha propios del test
 * para no chocar con otras cargas de la suite, y limpia solo lo que crea.
 */
@SpringBootTest
@ActiveProfiles("test")
class CargaMetricasIntegrationTest {
    private static final long ID_PARTIDO_TEST = 9_990_001L;
    private static final LocalDate FECHA_TEST = LocalDate.of(2026, 9, 1);

    @Autowired private CargaPartidoService cargaPartidoService;
    @Autowired private JugadorRepository jugadorRepository;
    @Autowired private MetricaPartidoRepository metricaRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private final List<Long> jugadoresCreados = new ArrayList<>();
    private Instant inicioDelTest;

    @BeforeEach
    void crearJugadoresDelMalaga() {
        inicioDelTest = Instant.now();
        jugadoresCreados.add(jugadorRepository.save(
                new Jugador("Alfonso Herrero", "Malaga", Liga.LA_LIGA, "POR")).getId());
        jugadoresCreados.add(jugadorRepository.save(
                new Jugador("Carlos Puga", "Malaga", Liga.LA_LIGA, "DEF")).getId());
    }

    @AfterEach
    void limpiarSoloLoQueCreoElTest() {
        String ids = jugadoresCreados.stream().map(String::valueOf)
                .reduce((primero, segundo) -> primero + "," + segundo).orElse("0");
        jdbc.update("DELETE FROM metrica_partido WHERE jugador_id IN (" + ids + ")");
        jdbc.update("DELETE FROM metrica_partido WHERE partido_externo_id IN "
                + "(SELECT id FROM partido_externo WHERE id_externo = ?)", String.valueOf(ID_PARTIDO_TEST));
        jdbc.update("DELETE FROM partido_externo WHERE id_externo = ?", String.valueOf(ID_PARTIDO_TEST));
        jdbc.update("DELETE FROM pendiente_vinculacion WHERE fecha_registro >= ?", Timestamp.from(inicioDelTest));
        jdbc.update("DELETE FROM tenencias WHERE jugador_id IN (" + ids + ")");
        jdbc.update("DELETE FROM jugadores WHERE id IN (" + ids + ")");
        jugadoresCreados.clear();
    }

    @Test
    void guardaLasMetricasDeLosJugadoresVinculadosYEsIdempotente() throws Exception {
        PartidoWhoScoredResumen resumen = new PartidoWhoScoredResumen(ID_PARTIDO_TEST,
                ZonedDateTime.of(FECHA_TEST.atTime(16, 0), ClockConfig.ZONA_NEGOCIO),
                "Malaga", "Espanyol", "FT", Liga.LA_LIGA);
        DatosPartidoWhoScored datos = datosDelFixture();

        var primera = cargaPartidoService.prepararYGuardar(resumen, datos);
        var segunda = cargaPartidoService.prepararYGuardar(resumen, datos);

        assertTrue(primera.cargado());
        assertEquals(2, primera.vinculados());
        assertEquals(30, primera.pendientes());
        assertFalse(segunda.cargado());
        assertTrue(cargaPartidoService.yaCargado(ID_PARTIDO_TEST));

        MetricaPartido herrero = metricaDe(jugadoresCreados.get(0));
        assertEquals(96, herrero.getMinutos().intValue());
        assertEquals(0, new BigDecimal("6.47").compareTo(herrero.getRating()));
        assertEquals(2, herrero.getAtajadas());
        assertEquals(1, herrero.getGolesRecibidos());
        assertNotNull(herrero.getPartidoExterno());

        MetricaPartido puga = metricaDe(jugadoresCreados.get(1));
        assertEquals(63, puga.getMinutos().intValue());
        assertEquals(1, puga.getAmarillas());
        assertEquals(2, puga.getEntradas());

        Integer metricasDelPartido = jdbc.queryForObject("SELECT COUNT(*) FROM metrica_partido m "
                + "JOIN partido_externo p ON p.id = m.partido_externo_id WHERE p.id_externo = ?",
                Integer.class, String.valueOf(ID_PARTIDO_TEST));
        assertEquals(2, metricasDelPartido.intValue());
        assertEquals("261509", jugadorRepository.findById(jugadoresCreados.get(0)).orElseThrow().getWhoscoredId());
    }

    private MetricaPartido metricaDe(Long jugadorId) {
        return metricaRepository.findByFechaPartidoBetween(FECHA_TEST, FECHA_TEST).stream()
                .filter(metrica -> metrica.getJugador().getId().equals(jugadorId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No hay métrica para el jugador " + jugadorId));
    }

    private DatosPartidoWhoScored datosDelFixture() throws Exception {
        JsonNode root = objectMapper.readTree(
                Files.readString(Path.of("src/test/resources/whoscored/LA_LIGA/partido-1994169.json")));
        return new DatosPartidoWhoScored(ID_PARTIDO_TEST, Liga.LA_LIGA, null,
                root.path("matchCentreData").toString(), root.path("matchCentreEventTypeJson").toString());
    }
}
