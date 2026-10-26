package com.formula11.integration;

import com.formula11.model.*;
import com.formula11.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class CotizacionRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private CotizacionRepository cotizaciones;
    @Autowired private JugadorRepository jugadores;
    @PersistenceContext private EntityManager entityManager;

    @BeforeEach
    void limpiar() {
        cotizaciones.deleteAll();
        jugadores.deleteAll();
    }

    @Test
    void consultaHistorialDescendenteYCotizacionVigente() {
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Jugador", "Equipo", Liga.LA_LIGA, "DEL"));
        cotizaciones.saveAllAndFlush(java.util.List.of(
                cotizacion(jugador, LocalDate.of(2026, 1, 4), "RENDIMIENTO_POSICION"),
                cotizacion(jugador, LocalDate.of(2026, 1, 11), "RENDIMIENTO_POSICION")));

        var vigente = cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                jugador.getId(), "RENDIMIENTO_POSICION", LocalDate.of(2026, 1, 8));

        assertEquals(LocalDate.of(2026, 1, 4), vigente.orElseThrow().getFecha());
        assertEquals(1, cotizaciones.findByFecha(LocalDate.of(2026, 1, 11)).size());
    }

    @Test
    void impideDosCotizacionesDeLaMismaSerieYFecha() {
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Jugador", "Equipo", Liga.LA_LIGA, "DEL"));
        cotizaciones.saveAndFlush(cotizacion(jugador, LocalDate.of(2026, 1, 4), "CONSISTENCIA"));

        assertThrows(DataIntegrityViolationException.class, () ->
                cotizaciones.saveAndFlush(cotizacion(jugador, LocalDate.of(2026, 1, 4), "CONSISTENCIA")));
    }

    @Test
    void permiteReemplazarLaSemanaEliminandoElRegistroAnterior() {
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Jugador", "Equipo", Liga.LA_LIGA, "DEL"));
        LocalDate fecha = LocalDate.of(2026, 1, 4);
        cotizaciones.saveAndFlush(cotizacion(jugador, fecha, "RENDIMIENTO_POSICION"));
        cotizaciones.deleteByFecha(fecha);
        entityManager.flush();
        entityManager.clear();
        cotizaciones.saveAndFlush(cotizacion(jugador, fecha, "RENDIMIENTO_POSICION"));

        assertEquals(1, cotizaciones.findByFecha(fecha).size());
    }

    private static Cotizacion cotizacion(Jugador jugador, LocalDate fecha, String estrategia) {
        return new Cotizacion(jugador, fecha, new BigDecimal("1.0000"), new BigDecimal("0.50000000"),
                estrategia, 1, Map.of("DEL", Map.of("goles", 1d)));
    }
}
