package com.formula11.integration;

import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.persistence.JugadorRepository;
import com.formula11.service.CotizacionConsultaService;
import com.formula11.service.CotizacionHistoricaPort;
import com.formula11.service.CotizacionLeida;
import com.formula11.service.HistorialCotizacionesResultado;
import com.formula11.service.RankingResultado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consultas de la 008 contra PostgreSQL real: ranking paginado con filtros y desempate, historial, vigente
 * y la implementación real de CotizacionHistoricaPort. Usa fechas de 2099 para no mezclarse con otros datos
 * y borra solo lo que crea.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class CotizacionConsultaIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final LocalDate D1 = LocalDate.of(2099, 1, 4);
    private static final LocalDate D2 = LocalDate.of(2099, 1, 11);
    private static final LocalDate D3 = LocalDate.of(2099, 1, 18);

    @Autowired private CotizacionConsultaService service;
    @Autowired private CotizacionHistoricaPort port;
    @Autowired private CotizacionRepository cotizaciones;
    @Autowired private JugadorRepository jugadores;
    @Autowired private EstrategiaRepository estrategias;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> creados = new ArrayList<>();
    private String activa;
    private String otra;
    private Jugador zeta;
    private Jugador alfa;
    private Jugador beta;

    @BeforeEach
    void sembrar() {
        activa = estrategias.findByActivaTrue().map(Estrategia::getCodigo).orElseThrow();
        otra = "RENDIMIENTO_POSICION".equals(activa) ? "CONSISTENCIA" : "RENDIMIENTO_POSICION";
        zeta = crear("Zeta Ocho", Liga.LA_LIGA, "DEL");
        alfa = crear("Alfa Ocho", Liga.LA_LIGA, "DEL");
        beta = crear("Beta Ocho", Liga.SERIE_A, "POR");

        guardar(zeta, D1, "0.9000", "0.40", activa);
        guardar(zeta, D2, "1.0000", "0.55", activa);
        guardar(alfa, D2, "1.5000", "0.70", activa);
        // Zeta y Alfa empatan en score y precio en D3: desempata el nombre.
        guardar(zeta, D3, "1.2000", "0.80", activa);
        guardar(alfa, D3, "1.2000", "0.80", activa);
        guardar(beta, D3, "1.0000", "0.90", activa);
        guardar(zeta, D3, "3.0000", "0.10", otra);
    }

    @AfterEach
    void limpiar() {
        String ids = creados.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0");
        jdbc.update("DELETE FROM cotizacion WHERE jugador_id IN (" + ids + ")");
        jdbc.update("DELETE FROM jugadores WHERE id IN (" + ids + ")");
        creados.clear();
    }

    private Jugador crear(String nombre, Liga liga, String posicion) {
        Jugador jugador = jugadores.saveAndFlush(new Jugador(nombre, "Equipo 008", liga, posicion));
        creados.add(jugador.getId());
        return jugador;
    }

    private void guardar(Jugador jugador, LocalDate fecha, String precio, String score, String estrategia) {
        cotizaciones.saveAndFlush(new Cotizacion(jugador, fecha, new BigDecimal(precio), new BigDecimal(score),
                estrategia, 1, Map.of()));
    }

    @Test
    void rankingOrdenaDesempataYPagina() {
        RankingResultado primera = service.ranking(null, null, null, 0, 2, false);

        assertEquals(activa, primera.estrategia());
        assertEquals(D3, primera.fecha());
        assertEquals(1, primera.version());
        assertEquals(3, primera.totalElements());
        assertEquals(2, primera.totalPages());
        assertEquals(beta.getId(), primera.items().get(0).jugador().getId());
        assertEquals(1, primera.items().get(0).puesto());
        assertNull(primera.items().get(0).cotizacion().variacion());
        assertEquals(alfa.getId(), primera.items().get(1).jugador().getId());
        assertEquals(new BigDecimal("-20.00"), primera.items().get(1).cotizacion().variacion());

        RankingResultado segunda = service.ranking(null, null, null, 1, 2, false);

        assertEquals(1, segunda.items().size());
        assertEquals(3, segunda.items().get(0).puesto());
        assertEquals(zeta.getId(), segunda.items().get(0).jugador().getId());
        assertEquals(new BigDecimal("20.00"), segunda.items().get(0).cotizacion().variacion());
    }

    @Test
    void rankingFiltraPorLigaYPosicionEnLaBase() {
        RankingResultado laLiga = service.ranking(null, "La Liga", "DEL", 0, 20, false);
        RankingResultado otraEstrategia = service.ranking(otra, null, null, 0, 20, true);

        assertEquals(2, laLiga.totalElements());
        assertEquals(List.of(alfa.getId(), zeta.getId()),
                laLiga.items().stream().map(item -> item.jugador().getId()).toList());
        assertEquals(1, otraEstrategia.totalElements());
        assertEquals(new BigDecimal("3.0000"), otraEstrategia.items().get(0).cotizacion().precio());
    }

    @Test
    void historialYVigenteLeenLaSerieDelJugador() {
        HistorialCotizacionesResultado historial = service.historial(zeta.getId(), null, null, null, false);
        CotizacionLeida vigente = service.vigente(zeta.getId(), null, D2.plusDays(3), false);

        assertEquals(List.of(D1, D2, D3), historial.cotizaciones().stream().map(CotizacionLeida::fecha).toList());
        assertNull(historial.cotizaciones().get(0).variacion());
        assertEquals(new BigDecimal("11.11"), historial.cotizaciones().get(1).variacion());
        assertEquals(new BigDecimal("20.00"), historial.cotizaciones().get(2).variacion());
        assertEquals(D2, vigente.fecha());
        assertEquals(new BigDecimal("11.11"), vigente.variacion());
    }

    @Test
    void elDetalleNoMuestraCotizacionesPosterioresAHoy() {
        assertNull(service.detalle(zeta.getId()).cotizacionActual());
    }

    @Test
    void elPuertoDevuelveFechasYPreciosRealesDeLaActiva() {
        assertEquals(List.of(D1, D2, D3), port.fechasCotizacion(D1, D3));
        assertEquals(List.of(D2), port.fechasCotizacion(D2, D2.plusDays(6)));

        Map<Long, BigDecimal> precios = port.preciosA(List.of(zeta.getId(), alfa.getId(), beta.getId(), -1L),
                D2.plusDays(1));

        assertEquals(new BigDecimal("1.0000"), precios.get(zeta.getId()));
        assertEquals(new BigDecimal("1.5000"), precios.get(alfa.getId()));
        assertEquals(new BigDecimal("1.0000"), precios.get(beta.getId()));
        assertEquals(new BigDecimal("1.0000"), precios.get(-1L));
        assertEquals(4, precios.size());
    }
}
