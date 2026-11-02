package com.formula11.integration;

import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Senal;
import com.formula11.model.senales.MedidorAciertos;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.SenalRepository;
import com.formula11.service.PaginaSenales;
import com.formula11.service.SenalLeida;
import com.formula11.service.SenalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generación, regeneración, listado y aciertos de señales contra PostgreSQL real.
 * El jugador "Atrasado" vale 1 y sus cinco pares con el mismo score valen 1,30: PRECIO_ATRASADO del 30 %.
 * El jugador "Divergente" vale 1 en la activa y 1,25 en la otra: ENTRE_ESTRATEGIAS del 25 %.
 * El jugador "Suplente" diverge mucho pero no jugó: no recibe señal.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SenalIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final LocalDate SEMANA = LocalDate.of(2026, 10, 25);
    private static final LocalDate SIGUIENTE = LocalDate.of(2026, 11, 1);

    @Autowired private SenalService service;
    @Autowired private SenalRepository senales;
    @Autowired private CotizacionRepository cotizaciones;
    @Autowired private JugadorRepository jugadores;
    @Autowired private EstrategiaRepository estrategias;
    @Autowired private JdbcTemplate jdbc;

    private final List<Long> creados = new ArrayList<>();
    private String activa;
    private String otra;
    private Jugador atrasado;
    private Jugador divergente;
    private Jugador suplente;

    @BeforeEach
    void sembrar() {
        activa = estrategias.findByActivaTrue().map(Estrategia::getCodigo).orElseThrow();
        otra = "RENDIMIENTO_POSICION".equals(activa) ? "CONSISTENCIA" : "RENDIMIENTO_POSICION";
        atrasado = crear("Atrasado", true);
        sembrarCotizaciones(atrasado, "1.0000", "0.80", "1.0000");
        for (int i = 1; i <= 5; i++) {
            Jugador par = crear("Par " + i, true);
            sembrarCotizaciones(par, "1.3000", "0.80", "1.3000");
        }
        divergente = crear("Divergente", true);
        sembrarCotizaciones(divergente, "1.0000", "0.20", "1.2500");
        suplente = crear("Suplente", false);
        sembrarCotizaciones(suplente, "1.0000", "0.00", "2.0000");
    }

    @AfterEach
    void limpiar() {
        String ids = creados.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbc.update("DELETE FROM senal WHERE jugador_id IN (" + ids + ")");
        jdbc.update("DELETE FROM cotizacion WHERE jugador_id IN (" + ids + ")");
        jdbc.update("DELETE FROM metrica_partido WHERE jugador_id IN (" + ids + ")");
        jdbc.update("DELETE FROM jugadores WHERE id IN (" + ids + ")");
        creados.clear();
    }

    private Jugador crear(String nombre, boolean jugo) {
        Jugador jugador = jugadores.saveAndFlush(new Jugador(nombre + " 010", "Equipo 010", Liga.LIGUE_1, "MED"));
        creados.add(jugador.getId());
        if (jugo) {
            jdbc.update("INSERT INTO metrica_partido (jugador_id, fecha_partido, minutos, titular, fuente) "
                    + "VALUES (?, ?, 90, true, 'test-010')", jugador.getId(), Date.valueOf(SEMANA.minusDays(1)));
        }
        return jugador;
    }

    private void sembrarCotizaciones(Jugador jugador, String precioActiva, String score, String precioOtra) {
        cotizaciones.saveAndFlush(cotizacion(jugador, SEMANA, precioActiva, score, activa));
        cotizaciones.saveAndFlush(cotizacion(jugador, SEMANA, precioOtra, score, otra));
    }

    private static Cotizacion cotizacion(Jugador jugador, LocalDate fecha, String precio, String score, String estrategia) {
        return new Cotizacion(jugador, fecha, new BigDecimal(precio), new BigDecimal(score), estrategia, 1, Map.of());
    }

    private Map<Long, Senal> senalesPorJugador() {
        return senales.findByFecha(SEMANA).stream()
                .collect(Collectors.toMap(senal -> senal.getJugador().getId(), Function.identity()));
    }

    @Test
    void generaLasDosClasesDeSenalSoloParaQuienesJugaron() {
        service.generar(SEMANA);

        Map<Long, Senal> generadas = senalesPorJugador();
        assertEquals(2, generadas.size());
        Senal precioAtrasado = generadas.get(atrasado.getId());
        assertEquals(Senal.Tipo.PRECIO_ATRASADO, precioAtrasado.getTipo());
        assertEquals(0, new BigDecimal("30").compareTo(precioAtrasado.getBrecha()));
        assertEquals(0, new BigDecimal("1.3").compareTo(precioAtrasado.getPrecioEstimado()));
        assertEquals(activa, precioAtrasado.getEstrategia());
        Senal entreEstrategias = generadas.get(divergente.getId());
        assertEquals(Senal.Tipo.ENTRE_ESTRATEGIAS, entreEstrategias.getTipo());
        assertEquals(0, new BigDecimal("25").compareTo(entreEstrategias.getBrecha()));
        assertFalse(generadas.containsKey(suplente.getId()));
    }

    @Test
    void regenerarConservaLosIdsYBorraLasQueYaNoAplican() {
        service.generar(SEMANA);
        Long idAtrasado = senalesPorJugador().get(atrasado.getId()).getId();

        service.generar(SEMANA);
        assertEquals(idAtrasado, senalesPorJugador().get(atrasado.getId()).getId());
        assertEquals(2, senalesPorJugador().size());

        jdbc.update("UPDATE cotizacion SET precio = 1.05 WHERE jugador_id = ? AND estrategia = ?",
                divergente.getId(), otra);
        service.generar(SEMANA);

        Map<Long, Senal> despues = senalesPorJugador();
        assertEquals(1, despues.size());
        assertEquals(idAtrasado, despues.get(atrasado.getId()).getId());
    }

    @Test
    void listaLasVigentesOrdenadasPorBrecha() {
        service.generar(SEMANA);

        PaginaSenales pagina = service.listar(true, 0, 20);

        assertEquals(2, pagina.totalElements());
        assertEquals(List.of(atrasado.getId(), divergente.getId()),
                pagina.content().stream().map(SenalLeida::jugador).map(Jugador::getId).toList());
        assertEquals("Atrasado 010", pagina.content().get(0).jugador().getNombre());
        assertEquals(2, service.listar(false, 0, 20).totalElements());
    }

    @Test
    void mideLosAciertosContraElMercadoDeLaSemanaSiguiente() {
        service.generar(SEMANA);
        for (Long id : creados) {
            Jugador jugador = jugadores.findById(id).orElseThrow();
            String precio = id.equals(atrasado.getId()) ? "1.2000" : precioActual(id);
            cotizaciones.saveAndFlush(cotizacion(jugador, SIGUIENTE, precio, "0.50", activa));
        }

        MedidorAciertos.Resultado resultado = service.aciertos();

        // Mercado: el atrasado +20 % y los otros siete 0 % -> +2,5 %.
        assertEquals(1, resultado.semanas().size());
        MedidorAciertos.SemanaMedida semana = resultado.semanas().get(0);
        assertEquals(SEMANA, semana.fecha());
        assertEquals(2, semana.senales());
        assertEquals(1, semana.aciertos());
        assertEquals(new BigDecimal("10.00"), semana.variacionSenalados());
        assertEquals(new BigDecimal("2.50"), semana.variacionMercado());
        assertEquals(new BigDecimal("50.00"), resultado.tasaAcierto());
        assertEquals(new BigDecimal("7.50"), resultado.diferenciaPromedio());
    }

    private String precioActual(Long jugadorId) {
        return cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(jugadorId, activa, SEMANA)
                .orElseThrow().getPrecio().toPlainString();
    }
}
