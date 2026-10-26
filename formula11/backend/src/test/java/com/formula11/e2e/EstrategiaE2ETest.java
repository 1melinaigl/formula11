package com.formula11.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class EstrategiaE2ETest {
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

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioAutenticadoPuedeConsultarConfiguracion() throws Exception {
        mockMvc.perform(get("/api/estrategias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].codigo").exists())
                .andExpect(jsonPath("$[0].version").isNumber());

        mockMvc.perform(get("/api/estrategias/RENDIMIENTO_POSICION/versiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("RENDIMIENTO_POSICION"));
    }

    @Test
    void configuracionRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/estrategias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioNoPuedeAdministrarYcodigoDesconocidoDevuelve404() throws Exception {
        mockMvc.perform(post("/api/estrategias/RENDIMIENTO_POSICION/versiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pesos":{"DEL":{"goles":1.0}},"parametros":{"k":0.4,"topeVariacion":0.2,"piso":0.1}}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.correlationId").exists());

        mockMvc.perform(get("/api/estrategias/NO_EXISTE/versiones"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @WithMockUser(roles = "SUPERUSUARIO")
    void superusuarioPuedeCrearVersionYRecibeEsquemaContractual() throws Exception {
        mockMvc.perform(post("/api/estrategias/CONSISTENCIA/versiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pesos":{"TODAS":{"titularidad":0.5,"suplencias":-0.5}},"parametros":{"k":0.4,"topeVariacion":0.2,"piso":0.1,"lambda":1.0,"ventanaPartidos":6.0,"minimoPartidos":4.0}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("CONSISTENCIA"))
                .andExpect(jsonPath("$.version").isNumber())
                .andExpect(jsonPath("$.activa").isBoolean())
                .andExpect(jsonPath("$.pesos").exists())
                .andExpect(jsonPath("$.parametros").exists());
    }
}
