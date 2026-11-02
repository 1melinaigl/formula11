package com.formula11.integration;

import com.formula11.model.*;
import com.formula11.persistence.*;
import com.formula11.service.MercadoService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MercadoConcurrenciaIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MercadoService mercadoService;
    @Autowired UsuarioRepository usuarios;
    @Autowired JugadorRepository jugadores;
    @Autowired CuentaRepository cuentas;
    @Autowired TenenciaRepository tenencias;
    @Autowired TransaccionRepository transacciones;
    @Autowired AuditoriaMercadoRepository auditorias;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

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
    void dosComprasSimultaneasCompitenPorUltimoTokenYSaldo() throws Exception {
        Usuario superusuario = new Usuario("Super", "concurrencia-super@example.com", "hash");
        superusuario.asignarRol(Rol.SUPERUSUARIO);
        usuarios.saveAndFlush(superusuario);
        Jugador jugador = jugadores.saveAndFlush(new Jugador("Concurrente", "Equipo", Liga.LA_LIGA, "DEL"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 1, BigDecimal.ONE));
        Usuario comprador = usuarios.saveAndFlush(new Usuario("Comprador", "concurrencia-user@example.com", "hash"));
        cuentas.saveAndFlush(new Cuenta(comprador, BigDecimal.ONE));
        tenencias.saveAndFlush(new Tenencia(comprador, jugador, 99, BigDecimal.ONE));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Boolean> compra = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            try {
                mercadoService.comprar(comprador.getId(), jugador.getId(), 1);
                return true;
            } catch (RuntimeException exception) {
                return false;
            }
        };
        Future<Boolean> primera = executor.submit(compra);
        Future<Boolean> segunda = executor.submit(compra);
        boolean unaExitosa = primera.get(30, TimeUnit.SECONDS);
        boolean otraExitosa = segunda.get(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(1, (unaExitosa ? 1 : 0) + (otraExitosa ? 1 : 0));
        Cuenta cuenta = cuentas.findByUsuarioId(comprador.getId()).orElseThrow();
        Tenencia usuario = tenencias.findByUsuarioIdAndJugadorId(comprador.getId(), jugador.getId()).orElseThrow();
        Tenencia stock = tenencias.findByUsuarioIdAndJugadorId(superusuario.getId(), jugador.getId()).orElseThrow();
        assertTrue(cuenta.getSaldo().signum() >= 0);
        assertTrue(usuario.getCantidad() >= 0);
        assertTrue(stock.getCantidad() >= 0);
        assertEquals(100, usuario.getCantidad() + stock.getCantidad());
        assertEquals(1, transacciones.count());
        assertEquals(1, auditorias.count());
    }
}
