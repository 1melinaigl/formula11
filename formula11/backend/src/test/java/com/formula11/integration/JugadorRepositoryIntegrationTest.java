package com.formula11.integration;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class JugadorRepositoryIntegrationTest {
    static {
        System.setProperty("api.version", "1.40");
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JugadorRepository jugadorRepository;

    @BeforeEach
    void limpiar() {
        jugadorRepository.deleteAll();
    }

    @Test
    void persisteLasCincoLigasYOrdenaPorNombre() {
        jugadorRepository.saveAllAndFlush(java.util.List.of(
                new Jugador("Zidane", "Equipo Z", Liga.LIGUE_1, "Mediocampista"),
                new Jugador("Alonso", "Equipo A", Liga.PREMIER_LEAGUE, "Defensa"),
                new Jugador("Baresi", "Equipo B", Liga.SERIE_A, "Defensa"),
                new Jugador("Kroos", "Equipo K", Liga.BUNDESLIGA, "Mediocampista"),
                new Jugador("Messi", "Equipo M", Liga.LA_LIGA, "Delantero")));

        var jugadores = jugadorRepository.findAllByOrderByNombreAsc();

        assertEquals(5, jugadores.size());
        assertEquals("Alonso", jugadores.getFirst().getNombre());
        assertEquals(java.util.Set.of(Liga.values()), jugadores.stream().map(Jugador::getLiga).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void catalogoVacioDevuelveListaVacia() {
        assertTrue(jugadorRepository.findAllByOrderByNombreAsc().isEmpty());
    }

    @Test
    void paginaFiltraOrdenaYCalculaTotalesEnLaBase() {
        jugadorRepository.saveAllAndFlush(java.util.List.of(
                new Jugador("Zidane", "Real Madrid", Liga.LA_LIGA, "Mediocampista"),
                new Jugador("Alonso", "Real Madrid", Liga.LA_LIGA, "Defensa"),
                new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero")));

        Specification<Jugador> filtro = (root, query, builder) -> builder.and(
                builder.equal(root.get("liga"), Liga.LA_LIGA),
                builder.like(builder.lower(root.get("equipo")), "%madrid%"));
        Page<Jugador> pagina = jugadorRepository.findAll(filtro, PageRequest.of(0, 1,
                Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id"))));

        assertEquals(1, pagina.getNumberOfElements());
        assertEquals("Alonso", pagina.getContent().getFirst().getNombre());
        assertEquals(2, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
    }

    @Test
    void paginaFueraDeRangoDevuelveContenidoVacio() {
        jugadorRepository.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));

        Page<Jugador> pagina = jugadorRepository.findAll((Specification<Jugador>) null, PageRequest.of(1, 1));

        assertTrue(pagina.getContent().isEmpty());
        assertEquals(1, pagina.getTotalElements());
        assertEquals(1, pagina.getTotalPages());
    }
}
