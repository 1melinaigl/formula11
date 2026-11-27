package com.formula11.integration.cotizaciones;

import com.formula11.service.cotizaciones.RecalculoService;
import com.formula11.service.cotizaciones.CotizacionCacheInvalidator;
import com.formula11.service.cotizaciones.CotizacionConsultaService;
import com.formula11.service.cotizaciones.RankingResultado;
import com.formula11.service.cotizaciones.RangoRecalculo;
import com.formula11.model.valuacion.CalculadoraPrecio;
import com.formula11.model.valuacion.ParametrosPrecio;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import com.formula11.persistence.cotizaciones.CotizacionRepository;
import com.formula11.persistence.cotizaciones.EstrategiaRepository;
import com.formula11.persistence.jugadores.JugadorRepository;
import com.formula11.persistence.metricas.MetricaPartidoRepository;
import com.formula11.model.cotizaciones.Cotizacion;
import com.formula11.model.cotizaciones.Estrategia;
import com.formula11.model.jugadores.Jugador;
import com.formula11.model.jugadores.Liga;
import com.formula11.model.metricas.MetricaPartido;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class RecalculoServiceIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.clock.fecha-fija", () -> "2026-01-26");
    }

    @Autowired private RecalculoService recalculo;
    @Autowired private CotizacionConsultaService consulta;
    @Autowired private CotizacionCacheInvalidator cacheInvalidator;
    @Autowired private JugadorRepository jugadores;
    @Autowired private MetricaPartidoRepository metricas;
    @Autowired private CotizacionRepository cotizaciones;
    @Autowired private EstrategiaRepository estrategias;

    @BeforeEach
    void limpiar() {
        cacheInvalidator.invalidate();
        List<Jugador> jugadoresDelTest = jugadoresDelSeed();
        Set<Long> idsJugadores = jugadoresDelTest.stream().map(Jugador::getId).collect(java.util.stream.Collectors.toSet());
        Set<LocalDate> fechasCotizacion = Set.of(
                LocalDate.of(2026, 1, 4),
                LocalDate.of(2026, 1, 11),
                LocalDate.of(2026, 1, 18));
        for (Estrategia estrategia : estrategias.findAll()) {
            for (Long idJugador : idsJugadores) {
                cotizaciones.deleteAll(cotizaciones.findByJugadorIdAndEstrategiaOrderByFechaAsc(
                                idJugador, estrategia.getCodigo()).stream()
                        .filter(cotizacion -> fechasCotizacion.contains(cotizacion.getFecha()))
                        .toList());
            }
        }
        metricas.deleteAll(metricas.findByFechaPartidoBetween(
                        LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 16)).stream()
                .filter(metrica -> idsJugadores.contains(metrica.getJugador().getId()))
                .filter(metrica -> "test".equals(metrica.getFuente()))
                .toList());
    }

    @Test
    void recalculaTresSemanasYEncadenaElPrecioDeCadaEstrategia() {
        List<Jugador> jugadoresCreados = jugadoresDelSeed();
        assertEquals(3, jugadoresCreados.size());
        List<LocalDate> domingos = List.of(
                LocalDate.of(2026, 1, 4),
                LocalDate.of(2026, 1, 11),
                LocalDate.of(2026, 1, 18));

        for (int semana = 0; semana < domingos.size(); semana++) {
            LocalDate fecha = domingos.get(semana).minusDays(2);
            for (int jugador = 0; jugador < jugadoresCreados.size(); jugador++) {
                metricas.save(metrica(jugadoresCreados.get(jugador), fecha, jugador + semana + 1));
            }
        }
        metricas.flush();

        var response = recalculo.recalcular(new RangoRecalculo(
                LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 18)));

        assertEquals(3, response.semanasCalculadas());
        assertEquals(jugadores.count() * estrategias.count() * response.semanasCalculadas(),
                cotizaciones.count());
        List<Estrategia> configuraciones = estrategias.findAll();
        assertEquals(2, configuraciones.size());
        for (Jugador jugador : jugadoresCreados) {
            for (Estrategia estrategia : configuraciones) {
                List<Cotizacion> serie = cotizaciones.findByJugadorIdAndEstrategiaOrderByFechaAsc(
                        jugador.getId(), estrategia.getCodigo());
                assertEquals(3, serie.size());
                for (int i = 0; i < serie.size(); i++) {
                    int semana = i;
                    BigDecimal anterior = i == 0 ? null : serie.get(i - 1).getPrecio();
                    ParametrosPrecio parametros = new ParametrosPrecio(
                            valor(estrategia, "k", 0.4),
                            valor(estrategia, "topeVariacion", 0.2),
                            valor(estrategia, "piso", 0.1));
                    var esperado = CalculadoraPrecio.calcular(anterior, serie.get(i).getScore(),
                            parametros, true).precio();
                    assertEquals(esperado, serie.get(i).getPrecio(),
                            () -> "Precio incorrecto para " + estrategia.getCodigo() + " semana " + semana);
                }
            }
        }
    }

    @Test
    void rankingCacheadoSeActualizaAlRecalcularCotizaciones() {
        List<Jugador> jugadoresConMetricas = jugadoresDelSeed();
        Jugador jugador = jugadoresConMetricas.get(0);
        LocalDate fechaMetrica = LocalDate.of(2026, 1, 2);
        RangoRecalculo rango = new RangoRecalculo(
                LocalDate.of(2026, 1, 4), LocalDate.of(2026, 1, 4));
        for (int indice = 0; indice < jugadoresConMetricas.size(); indice++) {
            metricas.save(metrica(jugadoresConMetricas.get(indice), fechaMetrica, indice + 1));
        }
        metricas.flush();
        recalculo.recalcular(rango);

        RankingResultado cacheado = consulta.ranking(null, jugador.getLiga().getNombreLegible(),
                jugador.getPosicion(), 0, 100, false);
        RankingResultado.Item itemCacheado = rankingItem(cacheado, jugador.getId());

        List<MetricaPartido> metricaOriginal = metricas
                .findByFechaPartidoBetween(fechaMetrica, fechaMetrica).stream()
                .filter(metrica -> metrica.getJugador().getId().equals(jugador.getId()))
                .filter(metrica -> "test".equals(metrica.getFuente()))
                .toList();
        metricas.deleteAll(metricaOriginal);
        metricas.flush();
        metricas.saveAndFlush(metrica(jugador, fechaMetrica, 20));
        recalculo.recalcular(rango);

        RankingResultado actualizado = consulta.ranking(null, jugador.getLiga().getNombreLegible(),
                jugador.getPosicion(), 0, 100, false);
        RankingResultado.Item itemActualizado = rankingItem(actualizado, jugador.getId());
        boolean cambioScore = itemCacheado.cotizacion().score()
                .compareTo(itemActualizado.cotizacion().score()) != 0;
        boolean cambioPrecio = itemCacheado.cotizacion().precio()
                .compareTo(itemActualizado.cotizacion().precio()) != 0;

        assertTrue(cambioScore || cambioPrecio,
                "El ranking recargado debe reflejar un score o precio distinto al resultado cacheado");
    }

    private static RankingResultado.Item rankingItem(RankingResultado ranking, Long jugadorId) {
        return ranking.items().stream()
                .filter(item -> item.jugador().getId().equals(jugadorId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("El jugador no aparece en el ranking"));
    }

    private static MetricaPartido metrica(Jugador jugador, LocalDate fecha, int valor) {
        return new MetricaPartido(jugador, fecha, 90, true,
                valor, 0, valor, 0, valor, 0, 0, 0, 0, 0,
                0, 0, new BigDecimal("7.0").add(BigDecimal.valueOf(valor).movePointLeft(1)), "test");
    }

    private List<Jugador> jugadoresDelSeed() {
        Set<Liga> ligasSeleccionadas = EnumSet.noneOf(Liga.class);
        return jugadores.findAllByOrderByNombreAsc().stream()
                .filter(jugador -> "DEL".equals(jugador.getPosicion()))
                .filter(jugador -> ligasSeleccionadas.add(jugador.getLiga()))
                .limit(3)
                .toList();
    }

    private static BigDecimal valor(Estrategia estrategia, String nombre, double fallback) {
        return BigDecimal.valueOf(estrategia.getParametros().getOrDefault(nombre, fallback));
    }
}
