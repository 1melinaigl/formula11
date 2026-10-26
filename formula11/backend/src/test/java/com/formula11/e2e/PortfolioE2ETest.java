package com.formula11.e2e;

import com.formula11.model.Cuenta;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Tenencia;
import com.formula11.model.Rol;
import com.formula11.model.Usuario;
import com.formula11.persistence.*;
import com.formula11.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class PortfolioE2ETest {
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
    @Autowired TenenciaRepository tenencias;
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
    void portfolioVacioResponde200() throws Exception {
        Usuario usuario = crearUsuario("portfolio-ok@example.com");
        cuentas.saveAndFlush(new Cuenta(usuario, new BigDecimal("100")));

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/portfolio")
                        .header("Authorization", "Bearer " + token(usuario)))
                .andExpect(status().isOk());
    }

    @Test
    void portfolioConPosicionesDevuelveLosDatosDelJugador() throws Exception {
        Usuario usuario = crearUsuario("portfolio-posiciones@example.com");
        cuentas.saveAndFlush(new Cuenta(usuario, new BigDecimal("80")));
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "DEL"));
        tenencias.saveAndFlush(new Tenencia(usuario, jugador, 20, BigDecimal.ONE));

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/portfolio")
                        .header("Authorization", "Bearer " + token(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posiciones.length()").value(1))
                .andExpect(jsonPath("$.posiciones[0].jugador.nombre").value("Messi"))
                .andExpect(jsonPath("$.posiciones[0].cantidad").value(20));
    }

    @Test
    void portfolioSinAutenticacionResponde401() throws Exception {
        mockMvc.perform(get("/api/usuarios/1/portfolio"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void portfolioDeOtroUsuarioResponde403() throws Exception {
        Usuario autenticado = crearUsuario("portfolio-owner@example.com");
        Usuario pedido = crearUsuario("portfolio-other@example.com");

        mockMvc.perform(get("/api/usuarios/" + pedido.getId() + "/portfolio")
                        .header("Authorization", "Bearer " + token(autenticado)))
                .andExpect(status().isForbidden());
    }

    @Test
    void portfolioDelSuperusuarioResponde403() throws Exception {
        Usuario superusuario = new Usuario("Super", "portfolio-super@example.com", "hash");
        superusuario.asignarRol(Rol.SUPERUSUARIO);
        usuarios.saveAndFlush(superusuario);

        mockMvc.perform(get("/api/usuarios/" + superusuario.getId() + "/portfolio")
                        .header("Authorization", "Bearer "
                                + jwtService.generarToken(superusuario.getId(), superusuario.getEmail(), Rol.SUPERUSUARIO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void portfolioSinCuentaResponde404() throws Exception {
        Usuario usuario = crearUsuario("portfolio-missing@example.com");

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/portfolio")
                        .header("Authorization", "Bearer " + token(usuario)))
                .andExpect(status().isNotFound());
    }

    private Usuario crearUsuario(String email) {
        return usuarios.saveAndFlush(new Usuario("Ana", email, "hash"));
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario.getId(), usuario.getEmail(), Rol.USUARIO);
    }
}
