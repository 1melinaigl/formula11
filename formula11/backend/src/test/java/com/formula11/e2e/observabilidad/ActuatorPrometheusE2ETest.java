package com.formula11.e2e.observabilidad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.model.jugadores.Jugador;
import com.formula11.model.jugadores.Liga;
import com.formula11.model.mercado.Tenencia;
import com.formula11.model.usuarios.Rol;
import com.formula11.model.usuarios.Usuario;
import com.formula11.persistence.jugadores.JugadorRepository;
import com.formula11.persistence.mercado.TenenciaRepository;
import com.formula11.persistence.usuarios.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
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

import java.math.BigDecimal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Las métricas de negocio en el formato de Prometheus, de punta a punta por HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@Testcontainers
@ActiveProfiles("e2e")
class ActuatorPrometheusE2ETest {
    static {
        System.setProperty("api.version", "1.40");
    }

    private static final String SUPERUSUARIO_EMAIL = "superusuario@test.com";
    private static final String SUPERUSUARIO_PASSWORD = "Superusuario123!";

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
    @Autowired private UsuarioRepository usuarios;
    @Autowired private JugadorRepository jugadores;
    @Autowired private TenenciaRepository tenencias;

    @Test
    void lasOperacionesDeMercadoAparecenPorTipoYUnaCompraSumaUna() throws Exception {
        String tokenSuperusuario = login(SUPERUSUARIO_EMAIL, SUPERUSUARIO_PASSWORD);
        String antes = prometheus(tokenSuperusuario);
        for (String tipo : new String[]{"compra", "venta", "p2p"}) {
            assertTrue(antes.contains("formula11_mercado_operaciones_total{") && valor(antes, tipo) >= 0,
                    "Falta el tipo " + tipo + " en:\n" + antes);
        }

        Usuario superusuario = usuarios.findByRol(Rol.SUPERUSUARIO).orElseThrow();
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Prometheus " + UUID.randomUUID(), "Equipo", Liga.LA_LIGA, "DEL"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 100, BigDecimal.ONE));
        mockMvc.perform(post("/api/ordenes/compra")
                        .header("Authorization", "Bearer " + tokenDeUsuarioNuevo())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":" + jugador.getId() + ",\"cantidad\":1}"))
                .andExpect(status().isCreated());

        String despues = prometheus(tokenSuperusuario);
        assertEquals(valor(antes, "compra") + 1, valor(despues, "compra"));
        assertEquals(valor(antes, "venta"), valor(despues, "venta"));
    }

    private String prometheus(String token) throws Exception {
        return mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Valor de la serie del tipo pedido; las etiquetas pueden venir en cualquier orden. */
    private static double valor(String texto, String tipo) {
        Matcher matcher = Pattern.compile("^formula11_mercado_operaciones_total\\{[^}]*tipo=\"" + tipo + "\"[^}]*} (\\S+)$",
                Pattern.MULTILINE).matcher(texto);
        if (!matcher.find()) {
            throw new AssertionError("No está la serie del tipo " + tipo + " en:\n" + texto);
        }
        return Double.parseDouble(matcher.group(1));
    }

    private String tokenDeUsuarioNuevo() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        String email = "prometheus-" + sufijo + "@example.com";
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
