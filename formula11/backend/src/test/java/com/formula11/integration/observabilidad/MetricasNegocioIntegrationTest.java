package com.formula11.integration.observabilidad;

import com.formula11.exception.OperacionNoValidaException;
import com.formula11.model.jugadores.Jugador;
import com.formula11.model.jugadores.Liga;
import com.formula11.model.mercado.Cuenta;
import com.formula11.model.mercado.Tenencia;
import com.formula11.model.usuarios.Rol;
import com.formula11.model.usuarios.Usuario;
import com.formula11.observabilidad.MetricasNegocio;
import com.formula11.persistence.jugadores.JugadorRepository;
import com.formula11.persistence.mercado.CuentaRepository;
import com.formula11.persistence.mercado.TenenciaRepository;
import com.formula11.persistence.usuarios.UsuarioRepository;
import com.formula11.service.cotizaciones.RangoRecalculo;
import com.formula11.service.cotizaciones.RecalculoService;
import com.formula11.service.mercado.MercadoService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Las métricas de negocio con los services reales (el aspecto envuelve los proxies). Usa su propia base
 * para no mezclar operaciones con otros tests, y compara antes y después porque el registry es del contexto.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MetricasNegocioIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MercadoService mercado;
    @Autowired private RecalculoService recalculo;
    @Autowired private MeterRegistry registry;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CuentaRepository cuentas;
    @Autowired private JugadorRepository jugadores;
    @Autowired private TenenciaRepository tenencias;

    private Jugador jugador;

    @BeforeEach
    void prepararJugadorConStock() {
        Usuario superusuario = usuarios.findByRol(Rol.SUPERUSUARIO).orElseThrow();
        jugador = jugadores.saveAndFlush(new Jugador("Metrica " + UUID.randomUUID(), "Equipo", Liga.LA_LIGA, "DEL"));
        tenencias.saveAndFlush(new Tenencia(superusuario, jugador, 100, BigDecimal.ONE));
    }

    @Test
    void unaCompraYUnaVentaConfirmadasSumanUnoACadaTipo() {
        Usuario usuario = usuarioConSaldo(new BigDecimal("100"));
        double comprasAntes = operaciones("compra");
        double ventasAntes = operaciones("venta");

        mercado.comprar(usuario.getId(), jugador.getId(), 2);
        mercado.vender(usuario.getId(), jugador.getId(), 1);

        assertEquals(comprasAntes + 1, operaciones("compra"));
        assertEquals(ventasAntes + 1, operaciones("venta"));
    }

    @Test
    void unaCompraRechazadaNoSuma() {
        Usuario sinSaldo = usuarioConSaldo(BigDecimal.ZERO);
        double comprasAntes = operaciones("compra");

        assertThrows(OperacionNoValidaException.class, () -> mercado.comprar(sinSaldo.getId(), jugador.getId(), 1));

        assertEquals(comprasAntes, operaciones("compra"));
    }

    @Test
    void cadaRecalculoSumaUnaMuestraConSuResultado() {
        long okAntes = recalculos("ok");
        long errorAntes = recalculos("error");

        recalculo.recalcular(null);
        RangoRecalculo invertido = new RangoRecalculo(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> recalculo.recalcular(invertido));

        assertEquals(okAntes + 1, recalculos("ok"));
        assertEquals(errorAntes + 1, recalculos("error"));
    }

    private Usuario usuarioConSaldo(BigDecimal saldo) {
        Usuario usuario = usuarios.saveAndFlush(new Usuario("Ana", "metricas-" + UUID.randomUUID() + "@example.com", "hash"));
        cuentas.saveAndFlush(new Cuenta(usuario, saldo));
        return usuario;
    }

    private double operaciones(String tipo) {
        return registry.get(MetricasNegocio.OPERACIONES_MERCADO).tag("tipo", tipo).counter().count();
    }

    private long recalculos(String resultado) {
        Timer timer = registry.find(MetricasNegocio.RECALCULO).tag("resultado", resultado).timer();
        return timer == null ? 0 : timer.count();
    }
}
