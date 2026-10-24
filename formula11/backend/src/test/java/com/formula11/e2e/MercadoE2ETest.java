package com.formula11.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.model.*;
import com.formula11.persistence.*;
import com.formula11.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class MercadoE2ETest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired JugadorRepository jugadores;
    @Autowired CuentaRepository cuentas;
    @Autowired TenenciaRepository tenencias;
    @Autowired TransaccionRepository transacciones;
    @Autowired AuditoriaMercadoRepository auditorias;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpiar() {
        jdbc.update("DELETE FROM auditoria_mercado");
        transacciones.deleteAll();
        tenencias.deleteAll();
        cuentas.deleteAll();
        usuarios.deleteAll();
        jugadores.deleteAll();
    }

    @Test
    void compraExitosaRegistraTransferenciaYAuditoria() throws Exception {
        Usuario superusuario = new Usuario("Super", "super-e2e@example.com", "hash");
        superusuario.asignarRol(Rol.SUPERUSUARIO);
        usuarios.saveAndFlush(superusuario);
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 100, BigDecimal.ONE));

        String registro = mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"ana-market@example.com\",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(registro);
        Usuario comprador = usuarios.findByEmail("ana-market@example.com").orElseThrow();
        mockMvc.perform(post("/api/ordenes/compra")
                        .header("Authorization", "Bearer " + json.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":" + jugador.getId() + ",\"cantidad\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("COMPRA"))
                .andExpect(jsonPath("$.cantidad").value(2))
                .andExpect(jsonPath("$.precioUnitario").value(1.0000))
                .andExpect(jsonPath("$.saldoPosterior").value(98.0000));
        Assertions.assertEquals(1, transacciones.count());
        Assertions.assertEquals(1, auditorias.count());
    }

    @Test
    void compraRechazaSolicitudInvalidaYSinAutenticacion() throws Exception {
        mockMvc.perform(post("/api/ordenes/compra")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":1,\"cantidad\":0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void compraRechazaSolicitudInvalidaAutenticadaCon400() throws Exception {
        String token = registrar("validacion-market@example.com", new BigDecimal("100"));
        mockMvc.perform(post("/api/ordenes/compra").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":1,\"cantidad\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compraRechazaSuperusuarioCon403() throws Exception {
        Usuario superusuario = crearSuperusuario("super-403@example.com");
        String token = jwtService.generarToken(superusuario.getId(), superusuario.getEmail(), Rol.SUPERUSUARIO);
        mockMvc.perform(post("/api/ordenes/compra").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":1,\"cantidad\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void compraRechazaJugadorInexistenteCon404() throws Exception {
        crearSuperusuario("super-404@example.com");
        String token = registrar("not-found-market@example.com", new BigDecimal("100"));
        mockMvc.perform(post("/api/ordenes/compra").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":999,\"cantidad\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void compraRechazaStockInsuficienteCon409() throws Exception {
        Usuario superusuario = crearSuperusuario("super-409@example.com");
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 0, BigDecimal.ONE));
        String token = registrar("stock-market@example.com", new BigDecimal("100"));
        mockMvc.perform(post("/api/ordenes/compra").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":" + jugador.getId() + ",\"cantidad\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void compraRechazaSaldoInsuficienteCon422() throws Exception {
        Usuario superusuario = crearSuperusuario("super-422@example.com");
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 100, BigDecimal.ONE));
        String token = registrar("balance-market@example.com", BigDecimal.ZERO);
        mockMvc.perform(post("/api/ordenes/compra").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":" + jugador.getId() + ",\"cantidad\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ventaExitosaAcreditaSaldoYConservaTenenciaEnCero() throws Exception {
        Usuario superusuario = crearSuperusuario("super-venta@example.com");
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 98, BigDecimal.ONE));
        String token = registrar("venta-market@example.com", new BigDecimal("100"));
        Usuario vendedor = usuarios.findByEmail("venta-market@example.com").orElseThrow();
        Tenencia tenencia = tenencias.saveAndFlush(new Tenencia(vendedor, jugador, 2, new BigDecimal("3.5000")));

        mockMvc.perform(post("/api/ordenes/venta").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":" + jugador.getId() + ",\"cantidad\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("VENTA"))
                .andExpect(jsonPath("$.saldoPosterior").value(102.0000));

        Assertions.assertEquals(1, transacciones.count());
        Assertions.assertEquals(1, auditorias.count());
        Assertions.assertEquals(0, tenencias.findById(tenencia.getId()).orElseThrow().getCantidad());
        Assertions.assertEquals(100, tenencias.findByUsuarioIdAndJugadorId(superusuario.getId(), jugador.getId()).orElseThrow().getCantidad());
    }

    @Test
    void ventaRechazaTenenciaInsuficienteCon422YSinCambios() throws Exception {
        Usuario superusuario = crearSuperusuario("super-venta-422@example.com");
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 98, BigDecimal.ONE));
        String token = registrar("venta-422@example.com", new BigDecimal("100"));
        Usuario vendedor = usuarios.findByEmail("venta-422@example.com").orElseThrow();
        tenencias.saveAndFlush(new Tenencia(vendedor, jugador, 1, BigDecimal.ONE));

        mockMvc.perform(post("/api/ordenes/venta").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":" + jugador.getId() + ",\"cantidad\":2}"))
                .andExpect(status().isUnprocessableEntity());

        Assertions.assertEquals(0, transacciones.count());
        Assertions.assertEquals(0, auditorias.count());
        Assertions.assertEquals(new BigDecimal("100.0000"), cuentas.findByUsuarioId(vendedor.getId()).orElseThrow().getSaldo());
    }

    @Test
    void ventaRechazaSuperusuarioCon403() throws Exception {
        Usuario superusuario = crearSuperusuario("super-venta-403@example.com");
        String token = jwtService.generarToken(superusuario.getId(), superusuario.getEmail(), Rol.SUPERUSUARIO);

        mockMvc.perform(post("/api/ordenes/venta").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":1,\"cantidad\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ventaRechazaSolicitudInvalidaCon400YFaltaDeAutenticacionCon401() throws Exception {
        String token = registrar("venta-validacion@example.com", new BigDecimal("100"));
        mockMvc.perform(post("/api/ordenes/venta").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":1,\"cantidad\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ordenes/venta")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"jugadorId\":1,\"cantidad\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaMercadoDevuelveJugadorStockYPrecio() throws Exception {
        Usuario superusuario = crearSuperusuario("super-mercado-query@example.com");
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 100, BigDecimal.ONE));
        String token = registrar("mercado-query@example.com", new BigDecimal("100"));

        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/mercado")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadorId").value(jugador.getId()))
                .andExpect(jsonPath("$.tokensEmitidos").value(100))
                .andExpect(jsonPath("$.tokensDisponibles").value(100))
                .andExpect(jsonPath("$.precioVigente").value(1.0000))
                .andExpect(jsonPath("$.fechaCotizacion").doesNotExist());
    }

    @Test
    void consultaMercadoRechazaJugadorInexistenteCon404() throws Exception {
        String token = registrar("mercado-not-found@example.com", new BigDecimal("100"));

        mockMvc.perform(get("/api/jugadores/999/mercado")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultaMercadoRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/jugadores/1/mercado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultaMercadoReflejaStockLuegoDeComprar() throws Exception {
        Usuario superusuario = crearSuperusuario("super-mercado-purchase@example.com");
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 100, BigDecimal.ONE));
        String token = registrar("mercado-purchase@example.com", new BigDecimal("100"));

        mockMvc.perform(post("/api/ordenes/compra")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jugadorId\":" + jugador.getId() + ",\"cantidad\":2}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/jugadores/" + jugador.getId() + "/mercado")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokensDisponibles").value(98));
    }

    @Test
    void registroCreaCuentaYLoginExponeRolEnRespuestaYJwt() throws Exception {
        String registro = mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Rol\",\"email\":\"rol-user@example.com\",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        Usuario usuario = usuarios.findByEmail("rol-user@example.com").orElseThrow();
        Assertions.assertEquals(Rol.USUARIO, usuario.getRol());
        Assertions.assertEquals(new BigDecimal("100.0000"), cuentas.findByUsuarioId(usuario.getId()).orElseThrow().getSaldo());

        String login = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rol-user@example.com\",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andReturn().getResponse().getContentAsString();
        Assertions.assertEquals(Rol.USUARIO, jwtService.extraerRol(objectMapper.readTree(login).get("token").asText()));
    }

    @Test
    void superusuarioNoTieneCuentaYLoginExponeRolEnRespuestaYJwt() throws Exception {
        Usuario superusuario = new Usuario("Super", "rol-super@example.com",
                passwordEncoder.encode("Secreto123!"));
        superusuario.asignarRol(Rol.SUPERUSUARIO);
        usuarios.saveAndFlush(superusuario);

        String login = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"rol-super@example.com\",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("SUPERUSUARIO"))
                .andReturn().getResponse().getContentAsString();
        Assertions.assertTrue(cuentas.findByUsuarioId(superusuario.getId()).isEmpty());
        Assertions.assertEquals(Rol.SUPERUSUARIO, jwtService.extraerRol(objectMapper.readTree(login).get("token").asText()));
    }

    private Usuario crearSuperusuario(String email) {
        Usuario usuario = new Usuario("Super", email, "hash");
        usuario.asignarRol(Rol.SUPERUSUARIO);
        return usuarios.saveAndFlush(usuario);
    }

    private String registrar(String email, BigDecimal saldo) throws Exception {
        String registro = mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"Secreto123!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        Cuenta cuenta = cuentas.findByUsuarioId(usuario.getId()).orElseThrow();
        if (saldo.compareTo(cuenta.getSaldo()) < 0) {
            cuenta.debitar(cuenta.getSaldo().subtract(saldo));
            cuentas.saveAndFlush(cuenta);
        }
        return objectMapper.readTree(registro).get("token").asText();
    }
}
