package com.formula11.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class RegistroCatalogoE2ETest {
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
    private JugadorRepository jugadorRepository;

    @BeforeEach
    void limpiarCatalogo() {
        jugadorRepository.deleteAll();
    }

    @Test
    void registroEmiteTokenYTokenPermiteConsultarCatalogo() throws Exception {
        String respuesta = mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Correlation-Id", "e2e-registro")
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana-e2e@example.com\",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "e2e-registro"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("ana-e2e@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(respuesta).get("token").asText();
        jugadorRepository.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));

        mockMvc.perform(get("/api/jugadores").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Messi"))
                .andExpect(jsonPath("$[0].liga").value("La Liga"))
                .andExpect(jsonPath("$[0].precio").doesNotExist());
    }

    @Test
    void rechazaAccesoSinTokenTokenInvalidoYVencido() throws Exception {
        mockMvc.perform(get("/api/jugadores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("La autenticación es requerida"));

        mockMvc.perform(get("/api/jugadores").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaRegistroInvalidoYEmailDuplicado() throws Exception {
        String body = "{\"nombre\":\"Ana\",\"email\":\"duplicado@example.com\",\"password\":\"Secreto123!\"}";
        mockMvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Otra\",\"email\":\" DUPLICADO@EXAMPLE.COM \",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/usuarios/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"email\":\"no-es-email\",\"password\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }
}
