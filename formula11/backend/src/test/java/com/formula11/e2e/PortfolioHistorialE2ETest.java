package com.formula11.e2e;

import com.formula11.model.AuditoriaMercado;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Transaccion;
import com.formula11.model.Usuario;
import com.formula11.persistence.AuditoriaMercadoRepository;
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
class PortfolioHistorialE2ETest {
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
    @Autowired JugadorRepository jugadores;
    @Autowired TransaccionRepository transacciones;
    @Autowired AuditoriaMercadoRepository auditorias;
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
    void paginaFiltraOrdenaYNoExponeOtroUsuario() throws Exception {
        Usuario usuario = crearUsuario("historial-owner@example.com");
        Usuario otro = crearUsuario("historial-other@example.com");
        Usuario contraparte = crearUsuario("historial-super@example.com");
        Jugador jugador = jugadores.saveAndFlush(
                new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        Instant primera = Instant.parse("2026-10-24T10:00:00Z");
        Instant segunda = Instant.parse("2026-10-25T10:00:00Z");
        guardarOperacion(usuario, contraparte, jugador, Transaccion.Tipo.COMPRA, primera, "98.0000");
        guardarOperacion(usuario, contraparte, jugador, Transaccion.Tipo.VENTA, segunda, "101.0000");
        guardarOperacion(otro, contraparte, jugador, Transaccion.Tipo.COMPRA, segunda, "77.0000");

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/transacciones")
                        .param("tipo", "COMPRA")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + token(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("COMPRA"))
                .andExpect(jsonPath("$.content[0].saldoPosterior").value(98.0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void jugadorInexistenteDevuelvePaginaVaciaYParametrosInvalidos400() throws Exception {
        Usuario usuario = crearUsuario("historial-validation@example.com");
        String token = token(usuario);

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/transacciones")
                        .param("jugadorId", "999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/transacciones")
                        .param("jugadorId", "0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/usuarios/" + usuario.getId() + "/transacciones")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historialRequiereAutenticacionYPropiedad() throws Exception {
        Usuario autenticado = crearUsuario("historial-auth@example.com");
        Usuario pedido = crearUsuario("historial-forbidden@example.com");

        mockMvc.perform(get("/api/usuarios/" + autenticado.getId() + "/transacciones"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/usuarios/" + pedido.getId() + "/transacciones")
                        .header("Authorization", "Bearer " + token(autenticado)))
                .andExpect(status().isForbidden());
    }

    private void guardarOperacion(Usuario usuario, Usuario contraparte, Jugador jugador,
                                  Transaccion.Tipo tipo, Instant fecha, String saldo) {
        Transaccion transaccion = transacciones.saveAndFlush(new Transaccion(
                tipo, usuario, jugador, 1, BigDecimal.ONE, contraparte,
                fecha, "activa", 1));
        auditorias.save(new AuditoriaMercado(
                usuario, transaccion, "{\"saldoUsuario\":100}",
                "{\"saldoUsuario\":" + saldo + "}", fecha));
    }

    private Usuario crearUsuario(String email) {
        return usuarios.saveAndFlush(new Usuario("Ana", email, "hash"));
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario.getId(), usuario.getEmail(), com.formula11.model.Rol.USUARIO);
    }
}
