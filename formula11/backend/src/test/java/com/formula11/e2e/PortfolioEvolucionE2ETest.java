package com.formula11.e2e;

import com.formula11.model.Cuenta;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Rol;
import com.formula11.model.Transaccion;
import com.formula11.model.Usuario;
import com.formula11.persistence.CuentaRepository;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.TransaccionRepository;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PortfolioEvolucionE2ETest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CuentaRepository cuentas;
    @Autowired JugadorRepository jugadores;
    @Autowired TransaccionRepository transacciones;
    @Autowired JwtService jwtService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM auditoria_mercado");
        jdbc.update("DELETE FROM transacciones");
        jdbc.update("DELETE FROM tenencias");
        jdbc.update("DELETE FROM cuentas");
        jdbc.update("DELETE FROM usuarios");
        jdbc.update("DELETE FROM jugadores");
    }

    @Test
    void evolucionReconstruyeTransaccionesEnUtc() throws Exception {
        Usuario usuario = crearUsuario("evolucion-ok@example.com");
        Usuario contraparte = crearUsuario("evolucion-super@example.com");
        Jugador jugador = jugadores.saveAndFlush(
                new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        cuentas.saveAndFlush(new Cuenta(usuario, new BigDecimal("100")));
        transacciones.saveAndFlush(new Transaccion(
                Transaccion.Tipo.COMPRA, usuario, jugador, 2, new BigDecimal("2.0000"),
                contraparte, Instant.parse("2026-10-25T23:30:00Z"), "activa", 1));

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/portfolio/evolucion")
                        .param("desde", "2026-10-26")
                        .param("hasta", "2026-10-26")
                        .header("Authorization", "Bearer " + token(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.puntos[0].fecha").value("2026-10-26"))
                .andExpect(jsonPath("$.puntos[0].invertido").value(4.0))
                .andExpect(jsonPath("$.puntos[0].valor").value(2.0));
    }

    @Test
    void evolucionSinCotizacionesDevuelveSerieVacia() throws Exception {
        Usuario usuario = crearUsuario("evolucion-empty@example.com");
        cuentas.saveAndFlush(new Cuenta(usuario, new BigDecimal("100")));

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/portfolio/evolucion")
                        .param("desde", "2026-10-27")
                        .param("hasta", "2026-10-27")
                        .header("Authorization", "Bearer " + token(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puntos").isEmpty());
    }

    @Test
    void evolucionValidaAutenticacionYFechas() throws Exception {
        mockMvc.perform(get("/api/usuarios/1/portfolio/evolucion")
                        .param("desde", "2026-10-26")
                        .param("hasta", "2026-10-25"))
                .andExpect(status().isUnauthorized());

        Usuario usuario = crearUsuario("evolucion-validation@example.com");
        cuentas.saveAndFlush(new Cuenta(usuario, new BigDecimal("100")));
        String token = token(usuario);

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/portfolio/evolucion")
                        .param("desde", "2026-10-26")
                        .param("hasta", "2026-10-25")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/portfolio/evolucion")
                        .param("desde", "fecha-invalida")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evolucionDeOtroUsuarioResponde403() throws Exception {
        Usuario autenticado = crearUsuario("evolucion-owner@example.com");
        Usuario pedido = crearUsuario("evolucion-other@example.com");
        cuentas.saveAndFlush(new Cuenta(autenticado, new BigDecimal("100")));

        mockMvc.perform(get("/api/usuarios/" + pedido.getId() + "/portfolio/evolucion")
                        .header("Authorization", "Bearer " + token(autenticado)))
                .andExpect(status().isForbidden());
    }

    private Usuario crearUsuario(String email) {
        return usuarios.saveAndFlush(new Usuario("Ana", email, "hash"));
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario.getId(), usuario.getEmail(), Rol.USUARIO);
    }
}
