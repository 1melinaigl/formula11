package com.formula11.e2e.observabilidad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.model.metricas.EjecucionMetricas;
import com.formula11.persistence.metricas.EjecucionMetricasRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("e2e")
class ActuatorHealthE2ETest {
    static {
        System.setProperty("api.version", "1.40");
    }

    private static final String SUPERUSUARIO_EMAIL = "superusuario@test.com";
    private static final String SUPERUSUARIO_PASSWORD = "Superusuario123!";
    private static final Instant INICIO = Instant.parse("2100-01-01T06:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EjecucionMetricasRepository ejecuciones;

    private final List<Long> creadas = new ArrayList<>();

    @AfterEach
    void borrarCargasDelTest() {
        ejecuciones.deleteAllById(creadas);
        creadas.clear();
    }

    @Test
    void sinSesionSoloSeVeElEstado() throws Exception {
        JsonNode health = leer(mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()));

        assertEquals("UP", health.get("status").asText());
        assertFalse(health.has("components"), health.toString());
    }

    @Test
    void unUsuarioComunTampocoVeElDetalle() throws Exception {
        String token = tokenDeUsuarioNuevo();

        JsonNode health = leer(mockMvc.perform(get("/actuator/health").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()));

        assertFalse(health.has("components"), health.toString());
    }

    @Test
    void elSuperusuarioVeElComponenteDeCargaDeMetricas() throws Exception {
        JsonNode health = leer(mockMvc.perform(get("/actuator/health")
                        .header("Authorization", "Bearer " + login(SUPERUSUARIO_EMAIL, SUPERUSUARIO_PASSWORD)))
                .andExpect(status().isOk()));

        JsonNode cargaMetricas = health.path("components").path("cargaMetricas");
        assertEquals("UP", cargaMetricas.path("status").asText(), health.toString());
        assertTrue(cargaMetricas.has("details"), health.toString());
    }

    @Test
    void conLaUltimaCargaFallidaElHealthPublicoEstaCaido() throws Exception {
        EjecucionMetricas fallida = new EjecucionMetricas("EN_CURSO", INICIO, "health-e2e");
        fallida.fallar(INICIO.plusSeconds(60), "Falla de prueba");
        creadas.add(ejecuciones.save(fallida).getId());

        JsonNode health = leer(mockMvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable()));

        assertEquals("DOWN", health.get("status").asText());
        assertFalse(health.has("components"), health.toString());
    }

    private JsonNode leer(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private String tokenDeUsuarioNuevo() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String email = "health-" + sufijo + "@example.com";
        String contrasena = "Clave-" + sufijo;
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"" + contrasena + "\"}"))
                .andExpect(status().isCreated());
        return login(email, contrasena);
    }

    private String login(String email, String contrasena) throws Exception {
        String respuesta = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + contrasena + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(respuesta).get("token").asText();
    }
}
