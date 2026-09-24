package com.formula11.integration;

import com.formula11.dto.LoginRequest;
import com.formula11.dto.RegistroUsuarioRequest;
import com.formula11.exception.CredencialesInvalidasException;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UsuarioLoginIntegrationTest {
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
    @Autowired
    private UsuarioService usuarioService;

    @BeforeEach
    void limpiarUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void loginBuscaEmailNormalizadoYVerificaPasswordConPostgres() {
        usuarioService.registrar(new RegistroUsuarioRequest("Ana", "ana-login@example.com", "Secreto123!"));

        var respuesta = usuarioService.login(new LoginRequest(" ANA-LOGIN@EXAMPLE.COM ", "Secreto123!"));

        assertEquals("ana-login@example.com", respuesta.email());
        assertEquals("Ana", respuesta.nombre());
    }

    @Test
    void loginRechazaPasswordIncorrecta() {
        usuarioService.registrar(new RegistroUsuarioRequest("Ana", "ana-login@example.com", "Secreto123!"));

        assertThrows(CredencialesInvalidasException.class,
                () -> usuarioService.login(new LoginRequest("ana-login@example.com", "Otra1234!")));
    }
}
