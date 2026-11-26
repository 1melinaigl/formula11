package com.formula11.e2e.observabilidad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.config.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("e2e")
class AuditoriaWebServicesE2ETest {
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
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Value("${app.auditoria.directorio}")
    private String directorioAuditoria;

    private String sufijo;
    private String email;
    private String contrasena;

    @BeforeEach
    void prepararUsuario() throws Exception {
        jdbc.update("DELETE FROM cuentas");
        jdbc.update("DELETE FROM usuarios WHERE rol = 'USUARIO'");
        sufijo = UUID.randomUUID().toString().substring(0, 8);
        email = "auditoria-" + sufijo + "@example.com";
        contrasena = "Clave-" + sufijo;
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"" + contrasena + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void elLoginQuedaEnElArchivoConElCorrelationIdYSinLaContrasena() throws Exception {
        String correlationId = "login-" + sufijo;

        String respuesta = mockMvc.perform(post("/api/usuarios/login")
                        .header(CorrelationIdFilter.HEADER, correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER, correlationId))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(respuesta).get("token").asText();

        List<String> lineas = lineasCon("[corr:" + correlationId + "]");
        assertEquals(1, lineas.size(), String.join("\n", lineas));
        String linea = lineas.get(0);
        assertTrue(linea.contains(" usuario=anónimo operacion=UsuarioController.login "), linea);
        assertTrue(linea.contains("\"password\":\"****\""), linea);
        assertTrue(linea.endsWith(" resultado=OK"), linea);

        String archivo = Files.readString(archivoAuditoria(), StandardCharsets.UTF_8);
        assertFalse(archivo.contains(contrasena), "La contraseña no puede quedar en el archivo de auditoría");
        assertFalse(archivo.contains(token), "El token no puede quedar en el archivo de auditoría");
    }

    @Test
    void unaLlamadaAutenticadaRegistraElEmailDelUsuario() throws Exception {
        String respuesta = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(respuesta).get("token").asText();
        String correlationId = "jugadores-" + sufijo;

        mockMvc.perform(get("/api/jugadores")
                        .header("Authorization", "Bearer " + token)
                        .header(CorrelationIdFilter.HEADER, correlationId))
                .andExpect(status().isOk());

        List<String> lineas = lineasCon("[corr:" + correlationId + "]");
        assertEquals(1, lineas.size(), String.join("\n", lineas));
        assertTrue(lineas.get(0).contains(" usuario=" + email + " operacion=JugadorController.listar "), lineas.get(0));
    }

    private Path archivoAuditoria() {
        return Path.of(directorioAuditoria, "auditoria.log");
    }

    private List<String> lineasCon(String texto) throws IOException {
        return Files.readAllLines(archivoAuditoria(), StandardCharsets.UTF_8).stream()
                .filter(linea -> linea.contains(texto))
                .toList();
    }
}
