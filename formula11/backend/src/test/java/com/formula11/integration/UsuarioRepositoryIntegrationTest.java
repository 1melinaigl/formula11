package com.formula11.integration;

import com.formula11.model.Usuario;
import com.formula11.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class UsuarioRepositoryIntegrationTest {
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
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void limpiar() {
        usuarioRepository.deleteAll();
    }

    @Test
    void persisteUsuarioConEmailNormalizado() {
        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(" Ana ", " ANA@EXAMPLE.COM ", "hash"));

        assertNotNull(usuario.getId());
        assertTrue(usuarioRepository.existsByEmail("ana@example.com"));
        assertEquals("ana@example.com", usuarioRepository.findByEmail("ana@example.com").orElseThrow().getEmail());
    }

    @Test
    void impideEmailDuplicadoEnBaseDeDatos() {
        usuarioRepository.saveAndFlush(new Usuario("Ana", "ana@example.com", "hash"));

        assertThrows(Exception.class, () -> usuarioRepository.saveAndFlush(new Usuario("Otra", " ANA@EXAMPLE.COM ", "otro-hash")));
    }
}
