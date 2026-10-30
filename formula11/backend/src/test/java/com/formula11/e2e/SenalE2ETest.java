package com.formula11.e2e;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Senal;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.SenalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SenalE2ETest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final LocalDate SEMANA_1 = LocalDate.of(2026, 10, 18);
    private static final LocalDate SEMANA_2 = LocalDate.of(2026, 10, 25);

    @Autowired private MockMvc mockMvc;
    @Autowired private JugadorRepository jugadores;
    @Autowired private SenalRepository senales;
    @Autowired private JdbcTemplate jdbc;

    private Jugador jugador;
    private Senal vigenteMayor;

    @BeforeEach
    void sembrar() {
        jugador = jugadores.saveAndFlush(new Jugador("Jugador E2E 010", "Equipo E2E", Liga.BUNDESLIGA, "DEF"));
        senales.saveAndFlush(senal(SEMANA_1, Senal.Tipo.ENTRE_ESTRATEGIAS, "40"));
        senales.saveAndFlush(senal(SEMANA_2, Senal.Tipo.ENTRE_ESTRATEGIAS, "16"));
        vigenteMayor = senales.saveAndFlush(senal(SEMANA_2, Senal.Tipo.PRECIO_ATRASADO, "22"));
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM senal WHERE jugador_id = ?", jugador.getId());
        jdbc.update("DELETE FROM jugadores WHERE id = ?", jugador.getId());
    }

    private Senal senal(LocalDate fecha, Senal.Tipo tipo, String brecha) {
        BigDecimal precio = new BigDecimal("1.0000");
        BigDecimal estimado = precio.multiply(BigDecimal.ONE.add(new BigDecimal(brecha).movePointLeft(2)));
        return new Senal(jugador, fecha, tipo, "RENDIMIENTO_POSICION", new BigDecimal(brecha), precio, estimado,
                Instant.parse("2026-10-26T09:00:00Z"));
    }

    @Test
    void sinJwtResponde401() throws Exception {
        mockMvc.perform(get("/api/senales")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/senales/aciertos")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void porDefectoDevuelveSoloLasDelUltimoRecalculo() throws Exception {
        mockMvc.perform(get("/api/senales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].id").value(vigenteMayor.getId()))
                .andExpect(jsonPath("$.content[0].tipo").value("PRECIO_ATRASADO"))
                .andExpect(jsonPath("$.content[0].fecha").value("2026-10-25"))
                .andExpect(jsonPath("$.content[0].brecha").value(22.0))
                .andExpect(jsonPath("$.content[0].precio").value(1.0))
                .andExpect(jsonPath("$.content[0].precioEstimado").value(1.22))
                .andExpect(jsonPath("$.content[0].jugador.nombre").value("Jugador E2E 010"))
                .andExpect(jsonPath("$.content[0].jugador.liga").value("Bundesliga"))
                .andExpect(jsonPath("$.content[1].tipo").value("ENTRE_ESTRATEGIAS"));
    }

    @Test
    @WithMockUser(roles = "SUPERUSUARIO")
    void conVigentesFalseDevuelveTodasDeLaMasNueva() throws Exception {
        mockMvc.perform(get("/api/senales").param("vigentes", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[2].fecha").value("2026-10-18"));
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void validaLaPaginacion() throws Exception {
        mockMvc.perform(get("/api/senales").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
        mockMvc.perform(get("/api/senales").param("vigentes", "quizas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void elPanelSinSemanaSiguienteDevuelveCeros() throws Exception {
        mockMvc.perform(get("/api/senales/aciertos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas").isEmpty())
                .andExpect(jsonPath("$.tasaAcierto").value(0.0))
                .andExpect(jsonPath("$.diferenciaPromedio").value(0.0));
    }
}
