package com.formula11.integration;

import com.formula11.model.*;
import com.formula11.persistence.*;
import com.formula11.service.SuperusuarioInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.model.AuditoriaMercado;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MercadoPersistenceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired UsuarioRepository usuarios;
    @Autowired CuentaRepository cuentas;
    @Autowired JugadorRepository jugadores;
    @Autowired TenenciaRepository tenencias;
    @Autowired SuperusuarioInitializer initializer;
    @Autowired TenenciaRepository tenenciaRepository;
    @Autowired AuditoriaMercadoRepository auditoriaRepository;
    @Autowired TransaccionRepository transaccionRepository;

    @Test
    void initializerEsIdempotenteYElSuperusuarioNoTieneCuenta() throws Exception {
        ApplicationArguments args = new DefaultApplicationArguments();
        initializer.run(args);
        long usuariosAntes = usuarios.findAll().stream().filter(Usuario::esSuperusuario).count();
        Usuario superusuarioAntes = usuarios.findByRol(Rol.SUPERUSUARIO).orElseThrow();
        long tenenciasAntes = tenenciaRepository.countByUsuarioId(superusuarioAntes.getId());

        initializer.run(args);

        assertEquals(1, usuarios.findAll().stream().filter(Usuario::esSuperusuario).count());
        assertEquals(usuariosAntes, usuarios.findAll().stream().filter(Usuario::esSuperusuario).count());
        Usuario superusuario = usuarios.findByRol(Rol.SUPERUSUARIO).orElseThrow();
        assertEquals(tenenciasAntes, tenenciaRepository.countByUsuarioId(superusuario.getId()));
        assertTrue(cuentas.findByUsuarioId(superusuario.getId()).isEmpty());
    }

    @Test
    void usuarioRegistradoTieneCuentaConSaldoInicialYRolUsuario() {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("Persistencia", "persistencia@example.com", "hash"));
        Cuenta cuenta = cuentas.saveAndFlush(new Cuenta(usuario, new BigDecimal("100.0000")));

        assertEquals(Rol.USUARIO, usuario.getRol());
        assertEquals(new BigDecimal("100.0000"), cuenta.getSaldo());
        assertEquals(usuario.getId(), cuentas.findByUsuarioId(usuario.getId()).orElseThrow().getUsuario().getId());
    }

    @Test
    void initializerAsignaCienTokensAUnJugadorNuevo() throws Exception {
        Usuario superusuario = usuarios.findByRol(Rol.SUPERUSUARIO).orElseThrow();
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Nuevo", "Equipo", Liga.LA_LIGA, "DEL"));

        initializer.run(new DefaultApplicationArguments());

        Tenencia tenencia = tenencias.findByUsuarioIdAndJugadorId(superusuario.getId(), jugador.getId()).orElseThrow();
        assertEquals(100, tenencia.getCantidad());
        assertEquals(new BigDecimal("1.0000"), tenencia.getPrecioPromedio());
    }

    @Test
    void indiceUnicoImpideDosSuperusuarios() {
        Usuario segundo = new Usuario("Super 2", "super-2@example.com", "hash");
        segundo.asignarRol(Rol.SUPERUSUARIO);

        assertThrows(Exception.class, () -> usuarios.saveAndFlush(segundo));
    }

    @Test
    void auditoriaEsAppendOnlyYConservaSnapshotsDeAmbosLados() throws Exception {
        assertTrue(java.util.Arrays.stream(AuditoriaMercadoRepository.class.getMethods())
                .noneMatch(metodo -> metodo.getName().startsWith("delete")), "La auditoría no debe exponer borrados");
        String antes = "{\"saldoUsuario\":100.0000,\"tokensUsuario\":2,\"tokensSuperusuario\":98}";
        String despues = "{\"saldoUsuario\":98.0000,\"tokensUsuario\":4,\"tokensSuperusuario\":96}";
        Usuario usuario = usuarios.saveAndFlush(new Usuario("Auditor", "auditor@example.com", "hash"));
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Auditable", "Equipo", Liga.LA_LIGA, "DEL"));
        Usuario superusuario = usuarios.findByRol(Rol.SUPERUSUARIO).orElseThrow();
        Transaccion transaccion = new Transaccion(Transaccion.Tipo.COMPRA, usuario, jugador, 2,
                BigDecimal.ONE, superusuario, java.time.Instant.now(), null, null);
        transaccionRepository.saveAndFlush(transaccion);
        AuditoriaMercado auditoria = auditoriaRepository.save(new AuditoriaMercado(
                usuario, transaccion, antes, despues, java.time.Instant.now()));
        assertEquals(1, auditoriaRepository.count());
        assertEquals(100.0000, new ObjectMapper().readTree(auditoria.getAntes()).get("saldoUsuario").doubleValue());
        assertEquals(2, new ObjectMapper().readTree(auditoria.getAntes()).get("tokensUsuario").intValue());
        assertEquals(98, new ObjectMapper().readTree(auditoria.getAntes()).get("tokensSuperusuario").intValue());
        assertEquals(98.0000, new ObjectMapper().readTree(auditoria.getDespues()).get("saldoUsuario").doubleValue());
    }
}
