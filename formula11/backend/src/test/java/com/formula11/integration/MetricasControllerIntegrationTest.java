package com.formula11.integration;

import com.formula11.security.JwtService;
import com.formula11.model.Rol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MetricasControllerIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;

    @Test
    void protegeMetricasYValidaPaginacion() throws Exception {
        String superusuario = jwtService.generarToken(1L, "super@test.com", Rol.SUPERUSUARIO);
        String usuario = jwtService.generarToken(2L, "user@test.com", Rol.USUARIO);

        mvc.perform(get("/api/metricas/pendientes").header("Authorization", "Bearer " + superusuario)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/metricas/pendientes").header("Authorization", "Bearer " + usuario))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/metricas/pendientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exponeElEstadoYElDisparoSoloAlSuperusuario() throws Exception {
        String token = jwtService.generarToken(1L, "super@test.com", Rol.SUPERUSUARIO);

        mvc.perform(get("/api/metricas/sincronizacion/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/metricas/sincronizacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"1900-01-01\"}"))
                .andExpect(status().isAccepted());
    }
}
