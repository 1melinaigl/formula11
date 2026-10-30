package com.formula11.e2e;

import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.persistence.JugadorRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fecha de negocio fija para que "hoy" sea el 28/10/2026 sin depender del reloj del CI.
@SpringBootTest(properties = "app.clock.fecha-fija=2026-10-28")
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CotizacionJugadorE2ETest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final LocalDate DOMINGO_1 = LocalDate.of(2026, 10, 18);
    private static final LocalDate DOMINGO_2 = LocalDate.of(2026, 10, 25);

    @Autowired private MockMvc mockMvc;
    @Autowired private JugadorRepository jugadores;
    @Autowired private CotizacionRepository cotizaciones;
    @Autowired private EstrategiaRepository estrategias;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> creados = new ArrayList<>();
    private String activa;
    private String otra;
    private Jugador jugador;

    @BeforeEach
    void sembrar() {
        activa = estrategias.findByActivaTrue().map(Estrategia::getCodigo).orElseThrow();
        otra = "RENDIMIENTO_POSICION".equals(activa) ? "CONSISTENCIA" : "RENDIMIENTO_POSICION";
        jugador = jugadores.saveAndFlush(new Jugador("Jugador E2E 008", "Equipo E2E", Liga.LA_LIGA, "MED"));
        creados.add(jugador.getId());
        guardar(DOMINGO_1, "1.0000", "0.50", activa);
        guardar(DOMINGO_2, "1.1000", "0.75", activa);
        guardar(DOMINGO_2, "0.9000", "0.20", otra);
    }

    @AfterEach
    void limpiar() {
        String ids = creados.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("0");
        jdbc.update("DELETE FROM cotizacion WHERE jugador_id IN (" + ids + ")");
        jdbc.update("DELETE FROM jugadores WHERE id IN (" + ids + ")");
        creados.clear();
    }

    private void guardar(LocalDate fecha, String precio, String score, String estrategia) {
        cotizaciones.saveAndFlush(new Cotizacion(jugador, fecha, new BigDecimal(precio), new BigDecimal(score),
                estrategia, 1, Map.of()));
    }

    @Test
    void sinJwtResponde401() throws Exception {
        mockMvc.perform(get("/api/jugadores/" + jugador.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/jugadores/ranking"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void detalleDevuelveElJugadorConSuCotizacionActual() throws Exception {
        mockMvc.perform(get("/api/jugadores/" + jugador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jugador.getId()))
                .andExpect(jsonPath("$.nombre").value("Jugador E2E 008"))
                .andExpect(jsonPath("$.liga").value("La Liga"))
                .andExpect(jsonPath("$.posicion").value("MED"))
                .andExpect(jsonPath("$.cotizacionActual.fecha").value("2026-10-25"))
                .andExpect(jsonPath("$.cotizacionActual.precio").value(1.1))
                .andExpect(jsonPath("$.cotizacionActual.variacion").value(10.0))
                .andExpect(jsonPath("$.cotizacionActual.estrategia").value(activa))
                .andExpect(jsonPath("$.cotizacionActual.version").value(1));
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void detalleValidaElIdYRespondeNoEncontrado() throws Exception {
        mockMvc.perform(get("/api/jugadores/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.correlationId").exists());
        mockMvc.perform(get("/api/jugadores/-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/jugadores/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void rankingNoEsCapturadoPorElDetalleYDevuelveLaPagina() throws Exception {
        mockMvc.perform(get("/api/jugadores/ranking").param("liga", "La Liga").param("posicion", "MED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estrategia").value(activa))
                .andExpect(jsonPath("$.fecha").value("2026-10-25"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].puesto").value(1))
                .andExpect(jsonPath("$.content[0].jugador.nombre").value("Jugador E2E 008"))
                .andExpect(jsonPath("$.content[0].variacion").value(10.0))
                .andExpect(jsonPath("$.content[0].score").value(0.75));
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void rankingValidaParametros() throws Exception {
        mockMvc.perform(get("/api/jugadores/ranking").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/jugadores/ranking").param("liga", "Liga Argentina"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/jugadores/ranking").param("estrategia", "NO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void unUsuarioNoPuedeConsultarOtraEstrategia() throws Exception {
        mockMvc.perform(get("/api/jugadores/ranking").param("estrategia", otra))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el superusuario puede consultar una estrategia que no es la activa"));
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones").param("estrategia", otra))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones/vigente").param("estrategia", otra))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERUSUARIO")
    void elSuperusuarioConsultaOtraEstrategia() throws Exception {
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones").param("estrategia", otra))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estrategia").value(otra))
                .andExpect(jsonPath("$.cotizaciones.length()").value(1))
                .andExpect(jsonPath("$.cotizaciones[0].precio").value(0.9))
                .andExpect(jsonPath("$.cotizaciones[0].variacion").value(nullValue()));
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void historialOrdenadoConRangoYValidaciones() throws Exception {
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadorId").value(jugador.getId()))
                .andExpect(jsonPath("$.estrategia").value(activa))
                .andExpect(jsonPath("$.cotizaciones[0].fecha").value("2026-10-18"))
                .andExpect(jsonPath("$.cotizaciones[0].variacion").value(nullValue()))
                .andExpect(jsonPath("$.cotizaciones[1].fecha").value("2026-10-25"))
                .andExpect(jsonPath("$.cotizaciones[1].variacion").value(10.0));
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones")
                        .param("desde", "2026-10-19").param("hasta", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotizaciones.length()").value(1))
                .andExpect(jsonPath("$.cotizaciones[0].variacion").value(10.0));
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones")
                        .param("desde", "2026-10-31").param("hasta", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones").param("desde", "ayer"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/jugadores/999999/cotizaciones"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void vigenteDevuelveLaUltimaHastaLaFechaPedida() throws Exception {
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones/vigente").param("fecha", "2026-10-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadorId").value(jugador.getId()))
                .andExpect(jsonPath("$.fecha").value("2026-10-18"))
                .andExpect(jsonPath("$.precio").value(1.0))
                .andExpect(jsonPath("$.variacion").value(nullValue()));
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones/vigente").param("fecha", "2026-01-01"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/cotizaciones/vigente").param("fecha", "no-es-fecha"))
                .andExpect(status().isBadRequest());
    }
}
