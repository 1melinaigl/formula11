package com.formula11.e2e.observabilidad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.config.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quién puede ver cada endpoint de Actuator. {@code @AutoConfigureObservability} hace que el test tenga
 * el registry de Prometheus, que Spring Boot apaga por defecto en los tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@Testcontainers
@ActiveProfiles("e2e")
class ActuatorSeguridadE2ETest {
    static {
        System.setProperty("api.version", "1.40");
    }

    private static final String SUPERUSUARIO_EMAIL = "superusuario@test.com";
    private static final String SUPERUSUARIO_PASSWORD = "Superusuario123!";
    private static final String LOGGER_AUDITORIA = "/actuator/loggers/AUDITORIA";

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
    @Value("${app.auditoria.directorio}")
    private String directorioAuditoria;

    private String tokenUsuario;
    private String tokenSuperusuario;

    @BeforeEach
    void iniciarSesiones() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String email = "actuator-" + sufijo + "@example.com";
        String contrasena = "Clave-" + sufijo;
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"" + contrasena + "\"}"))
                .andExpect(status().isCreated());
        tokenUsuario = login(email, contrasena);
        tokenSuperusuario = login(SUPERUSUARIO_EMAIL, SUPERUSUARIO_PASSWORD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/info", "/actuator/metrics", "/actuator/prometheus", "/actuator/loggers"})
    void sinSesionResponde401ConElErrorDeLaApi(String endpoint) throws Exception {
        JsonNode error = leer(mockMvc.perform(get(endpoint)).andExpect(status().isUnauthorized()));

        assertEquals("La autenticación es requerida", error.path("mensaje").asText());
        assertFalse(error.path("correlationId").asText().isBlank(), error.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/info", "/actuator/metrics", "/actuator/prometheus", "/actuator/loggers"})
    void unUsuarioComunRecibe403ConElErrorDeLaApi(String endpoint) throws Exception {
        JsonNode error = leer(mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + tokenUsuario))
                .andExpect(status().isForbidden()));

        assertEquals("No tiene permisos para realizar esta operación", error.path("mensaje").asText());
        assertFalse(error.path("correlationId").asText().isBlank(), error.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/info", "/actuator/metrics", "/actuator/prometheus", "/actuator/loggers"})
    void elSuperusuarioPuedeVerlo(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + tokenSuperusuario))
                .andExpect(status().isOk());
    }

    @Test
    void elInfoTieneElNombreYLaVersionDelBuild() throws Exception {
        JsonNode info = leer(mockMvc.perform(get("/actuator/info")
                        .header("Authorization", "Bearer " + tokenSuperusuario))
                .andExpect(status().isOk()));

        assertEquals("Formula11", info.path("app").path("nombre").asText(), info.toString());
        String version = info.path("app").path("version").asText();
        assertFalse(version.isBlank(), info.toString());
        assertFalse(version.contains("@"), "La versión tiene que venir filtrada por Maven: " + version);
    }

    @Test
    void losEndpointsNoExpuestosNoSeVenNiSiendoSuperusuario() throws Exception {
        int estado = mockMvc.perform(get("/actuator/env").header("Authorization", "Bearer " + tokenSuperusuario))
                .andReturn().getResponse().getStatus();

        assertNotEquals(200, estado);
    }

    @Test
    void unUsuarioComunNoPuedeCambiarElNivelDeLog() throws Exception {
        mockMvc.perform(post(LOGGER_AUDITORIA)
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configuredLevel\":\"WARN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elSuperusuarioApagaLaAuditoriaEnCalienteYLaVuelveAPrender() throws Exception {
        String nivelAnterior = leer(mockMvc.perform(get(LOGGER_AUDITORIA)
                        .header("Authorization", "Bearer " + tokenSuperusuario))
                .andExpect(status().isOk())).path("configuredLevel").asText("INFO");
        try {
            cambiarNivel("WARN");
            String apagada = "actuator-warn-" + UUID.randomUUID();
            llamarJugadores(apagada);
            assertFalse(archivoAuditoria().contains("[corr:" + apagada + "]"),
                    "Con AUDITORIA en WARN no se escriben líneas de auditoría");
        } finally {
            // Se restaura el nivel explícito: con null heredaría el root, que en el perfil e2e es WARN.
            cambiarNivel(nivelAnterior);
        }

        String prendida = "actuator-info-" + UUID.randomUUID();
        llamarJugadores(prendida);
        assertTrue(archivoAuditoria().contains("[corr:" + prendida + "]"), "Con AUDITORIA en INFO vuelve a auditar");
    }

    private void cambiarNivel(String nivel) throws Exception {
        mockMvc.perform(post(LOGGER_AUDITORIA)
                        .header("Authorization", "Bearer " + tokenSuperusuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configuredLevel\":\"" + nivel + "\"}"))
                .andExpect(status().isNoContent());
    }

    private void llamarJugadores(String correlationId) throws Exception {
        mockMvc.perform(get("/api/jugadores")
                        .header("Authorization", "Bearer " + tokenUsuario)
                        .header(CorrelationIdFilter.HEADER, correlationId))
                .andExpect(status().isOk());
    }

    private String archivoAuditoria() throws Exception {
        Path archivo = Path.of(directorioAuditoria, "auditoria.log");
        return Files.exists(archivo) ? Files.readString(archivo, StandardCharsets.UTF_8) : "";
    }

    private JsonNode leer(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
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
