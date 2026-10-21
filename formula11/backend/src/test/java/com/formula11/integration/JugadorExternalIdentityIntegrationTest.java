package com.formula11.integration;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class JugadorExternalIdentityIntegrationTest {
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
    void encuentraLaMismaIdentidadExternaSinDuplicar() {
        jugadorRepository.saveAndFlush(new Jugador("Jugador Uno", "Equipo", Liga.LA_LIGA, "DEL", "FOOTBALL_DATA", "p-1"));

        var existente = jugadorRepository.findByExternalProviderAndExternalId("FOOTBALL_DATA", "p-1");

        assertTrue(existente.isPresent());
        existente.get().actualizarDesdeExterno("FOOTBALL_DATA", "p-1", "Jugador Actualizado", "Equipo Nuevo", Liga.LA_LIGA, "MED");
        jugadorRepository.saveAndFlush(existente.get());

        assertEquals(1, jugadorRepository.count());
        assertEquals("Jugador Actualizado", jugadorRepository.findByExternalProviderAndExternalId("FOOTBALL_DATA", "p-1").orElseThrow().getNombre());
    }
}
