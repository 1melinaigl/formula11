package com.formula11.integration;

import com.formula11.model.*;
import com.formula11.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class MetricaPartidoRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MetricaPartidoRepository metricas;
    @Autowired private JugadorRepository jugadores;

    @BeforeEach
    void limpiar() {
        metricas.deleteAll();
        jugadores.deleteAll();
    }

    @Test
    void impideDosMetricasDelMismoJugadorEnLaMismaFecha() {
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Jugador", "Equipo", Liga.LA_LIGA, "DEL"));
        metricas.saveAndFlush(metrica(jugador, LocalDate.of(2026, 1, 4)));

        assertThrows(DataIntegrityViolationException.class,
                () -> metricas.saveAndFlush(metrica(jugador, LocalDate.of(2026, 1, 4))));
    }

    @Test
    void consultaTodasLasMetricasDeUnaSemanaEnUnaOperacionDeRepositorio() {
        Jugador primero = jugadores.save(new Jugador("Primero", "Equipo", Liga.LA_LIGA, "DEL"));
        Jugador segundo = jugadores.save(new Jugador("Segundo", "Equipo", Liga.LA_LIGA, "DEF"));
        metricas.saveAllAndFlush(java.util.List.of(
                metrica(primero, LocalDate.of(2026, 1, 5)),
                metrica(segundo, LocalDate.of(2026, 1, 11)),
                metrica(primero, LocalDate.of(2026, 1, 12))));

        var resultado = metricas.findByFechaPartidoBetween(
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 11));

        assertEquals(2, resultado.size());
        assertEquals(java.util.Set.of(primero.getId(), segundo.getId()),
                resultado.stream().map(m -> m.getJugador().getId()).collect(java.util.stream.Collectors.toSet()));
    }

    private static MetricaPartido metrica(Jugador jugador, LocalDate fecha) {
        return new MetricaPartido(jugador, fecha, 90, true, 1, 0, 2, 1, 0,
                0, 0, 0, 0, 0, 0, 1, BigDecimal.valueOf(8), "test");
    }
}
