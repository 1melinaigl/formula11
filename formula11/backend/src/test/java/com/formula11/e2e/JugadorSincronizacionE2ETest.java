package com.formula11.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.ExternalPlayer;
import com.formula11.adapter.ExternalTeam;
import com.formula11.adapter.FootballDataPort;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.TenenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class JugadorSincronizacionE2ETest {
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
    @Autowired
    private TenenciaRepository tenenciaRepository;
    @MockitoBean
    private FootballDataPort footballDataPort;

    @BeforeEach
    void limpiar() {
        tenenciaRepository.deleteAll();
        jugadorRepository.deleteAll();
        when(footballDataPort.estaConfigurado()).thenReturn(true);
        when(footballDataPort.obtenerEquipos(any(Liga.class))).thenAnswer(invocation -> {
            Liga liga = invocation.getArgument(0);
            return List.of(new ExternalTeam("team-" + liga.name(), "Equipo", liga.name()));
        });
        when(footballDataPort.obtenerJugadores(any(ExternalTeam.class))).thenAnswer(invocation -> {
            ExternalTeam equipo = invocation.getArgument(0);
            return List.of(new ExternalPlayer("player-" + equipo.liga(), "Jugador", equipo.nombre(), equipo.liga(), "DEL"));
        });
    }

    @Test
    void sincronizacionProtegidaDevuelveResumenPorLiga() throws Exception {
        String registro = mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Sync\",\"email\":\"sync-e2e@example.com\",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registro).get("token").asText();

        mockMvc.perform(post("/api/jugadores/sincronizacion")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Correlation-Id", "sync-e2e"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.ligas.length()").value(5))
                .andExpect(jsonPath("$.creados").value(5))
                .andExpect(jsonPath("$.correlationId").value("sync-e2e"));
    }
}
