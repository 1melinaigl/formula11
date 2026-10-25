package com.formula11.integration;

import com.formula11.model.Estrategia;
import com.formula11.persistence.EstrategiaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class EstrategiaRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private EstrategiaRepository estrategias;

    @Test
    void cargaSeedsYEstrategiaActivaPorDefecto() {
        assertEquals(2, estrategias.count());
        assertEquals("RENDIMIENTO_POSICION", estrategias.findByActivaTrue().orElseThrow().getCodigo());
    }

    @Test
    void devuelveVersionesEnOrdenDescendenteYConservaLaAnterior() {
        estrategias.saveAndFlush(new Estrategia("RENDIMIENTO_POSICION", "Rendimiento v2", 2,
                Map.of("DEL", Map.of("goles", 1d)), Map.of("k", 0.4d), false, Instant.now()));

        var versiones = estrategias.findByCodigoOrderByVersionDesc("RENDIMIENTO_POSICION");

        assertEquals(2, versiones.size());
        assertEquals(2, versiones.get(0).getVersion());
        assertEquals(1, versiones.get(1).getVersion());
    }

    @Test
    void impideDosEstrategiasActivas() {
        Estrategia otra = new Estrategia("CONSISTENCIA", "Consistencia v2", 2,
                Map.of("TODAS", Map.of("titularidad", 0.5d)),
                Map.of("k", 0.4d), true, Instant.now());

        assertThrows(DataIntegrityViolationException.class, () -> estrategias.saveAndFlush(otra));
    }
}
