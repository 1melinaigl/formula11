package com.formula11.unit.service.cotizaciones;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.exception.SoloSuperusuarioException;
import com.formula11.model.cotizaciones.Cotizacion;
import com.formula11.model.cotizaciones.Estrategia;
import com.formula11.model.jugadores.Jugador;
import com.formula11.model.jugadores.Liga;
import com.formula11.persistence.cotizaciones.CotizacionRepository;
import com.formula11.persistence.cotizaciones.EstrategiaRepository;
import com.formula11.persistence.jugadores.JugadorRepository;
import com.formula11.service.cotizaciones.CotizacionConsultaService;
import com.formula11.service.cotizaciones.CotizacionCacheInvalidator;
import com.formula11.service.cotizaciones.CotizacionLeida;
import com.formula11.service.cotizaciones.CotizacionVigentePort;
import com.formula11.service.cotizaciones.HistorialCotizacionesResultado;
import com.formula11.service.cotizaciones.JugadorDetalleResultado;
import com.formula11.service.cotizaciones.RankingClave;
import com.formula11.service.cotizaciones.RankingResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CotizacionConsultaServiceTest {
    private static final String ACTIVA = "RENDIMIENTO_POSICION";
    private static final String OTRA = "CONSISTENCIA";
    // 02:00 UTC del 29/10 son las 23:00 del 28/10 en Buenos Aires: la fecha de negocio es el 28.
    private static final Instant AHORA = Instant.parse("2026-10-29T02:00:00Z");
    private static final LocalDate HOY_NEGOCIO = LocalDate.of(2026, 10, 28);
    private static final LocalDate DOMINGO_1 = LocalDate.of(2026, 10, 11);
    private static final LocalDate DOMINGO_2 = LocalDate.of(2026, 10, 18);
    private static final LocalDate DOMINGO_3 = LocalDate.of(2026, 10, 25);

    private JugadorRepository jugadores;
    private CotizacionRepository cotizaciones;
    private EstrategiaRepository estrategias;
    private CotizacionConsultaService service;
    private Cache<RankingClave, RankingResultado> rankingCache;
    private Jugador jugador;

    @BeforeEach
    void setUp() {
        jugadores = mock(JugadorRepository.class);
        cotizaciones = mock(CotizacionRepository.class);
        estrategias = mock(EstrategiaRepository.class);
        rankingCache = Caffeine.newBuilder().build();
        service = new CotizacionConsultaService(jugadores, cotizaciones, estrategias,
                Clock.fixed(AHORA, ZoneOffset.UTC), rankingCache);
        Estrategia activa = mock(Estrategia.class);
        when(activa.getCodigo()).thenReturn(ACTIVA);
        when(estrategias.findByActivaTrue()).thenReturn(Optional.of(activa));
        jugador = jugador(7L, "Lamine Yamal");
        when(jugadores.findById(7L)).thenReturn(Optional.of(jugador));
        when(jugadores.findById(99L)).thenReturn(Optional.empty());
    }

    private static Jugador jugador(long id, String nombre) {
        Jugador mock = mock(Jugador.class);
        when(mock.getId()).thenReturn(id);
        when(mock.getNombre()).thenReturn(nombre);
        when(mock.getEquipo()).thenReturn("Barcelona");
        when(mock.getLiga()).thenReturn(Liga.LA_LIGA);
        when(mock.getPosicion()).thenReturn("DEL");
        return mock;
    }

    private static Cotizacion cotizacion(Jugador jugador, LocalDate fecha, String precio, String score, String estrategia) {
        return new Cotizacion(jugador, fecha, new BigDecimal(precio), new BigDecimal(score), estrategia, 1, Map.of());
    }

    // ---------------------------------------------------------------- estrategia

    @Test
    void sinEstrategiaUsaLaActiva() {
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        HistorialCotizacionesResultado resultado = service.historial(7L, null, null, null, 0, 20, false);

        assertEquals(ACTIVA, resultado.estrategia());
        assertTrue(resultado.cotizaciones().isEmpty());
    }

    @Test
    void unUsuarioPuedePedirExplicitamenteLaActiva() {
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        assertEquals(ACTIVA, service.historial(7L, ACTIVA, null, null, 0, 20, false).estrategia());
    }

    @Test
    void unUsuarioNoPuedePedirOtraEstrategia() {
        SoloSuperusuarioException error = assertThrows(SoloSuperusuarioException.class,
                () -> service.historial(7L, OTRA, null, null, 0, 20, false));

        assertTrue(error.getMessage().contains("superusuario"));
        verifyNoInteractions(cotizaciones);
    }

    @Test
    void elSuperusuarioPuedePedirOtraEstrategia() {
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        assertEquals(OTRA, service.historial(7L, OTRA, null, null, 0, 20, true).estrategia());
    }

    @Test
    void rechazaUnaEstrategiaQueNoExiste() {
        assertThrows(IllegalArgumentException.class, () -> service.historial(7L, "OTRA_COSA", null, null, 0, 20, true));
        assertThrows(IllegalArgumentException.class, () -> service.vigente(7L, " ", null, true));
    }

    @Test
    void sinEstrategiaActivaElHistorialEsVacio() {
        when(estrategias.findByActivaTrue()).thenReturn(Optional.empty());

        HistorialCotizacionesResultado resultado = service.historial(7L, null, null, null, 0, 20, false);

        assertNull(resultado.estrategia());
        assertTrue(resultado.cotizaciones().isEmpty());
    }

    // ---------------------------------------------------------------- detalle

    @Test
    void detalleDevuelveLaUltimaCotizacionDeLaActivaConVariacion() {
        Cotizacion actual = cotizacion(jugador, DOMINGO_3, "1.1000", "0.73456789", ACTIVA);
        Cotizacion anterior = cotizacion(jugador, DOMINGO_2, "1.0000", "0.5", ACTIVA);
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(7L, ACTIVA, HOY_NEGOCIO))
                .thenReturn(Optional.of(actual));
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                7L, ACTIVA, DOMINGO_3.minusDays(1))).thenReturn(Optional.of(anterior));

        JugadorDetalleResultado detalle = service.detalle(7L);

        assertSame(jugador, detalle.jugador());
        CotizacionLeida leida = detalle.cotizacionActual();
        assertEquals(DOMINGO_3, leida.fecha());
        assertEquals(new BigDecimal("1.1000"), leida.precio());
        assertEquals(new BigDecimal("0.7346"), leida.score());
        assertEquals(new BigDecimal("10.00"), leida.variacion());
        assertEquals(ACTIVA, leida.estrategia());
        assertEquals(1, leida.version());
        assertEquals(7L, leida.jugadorId());
    }

    @Test
    void detalleSinCotizacionesDevuelveNull() {
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(anyLong(), any(), any()))
                .thenReturn(Optional.empty());

        assertNull(service.detalle(7L).cotizacionActual());
    }

    @Test
    void detalleDeJugadorInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.detalle(99L));
    }

    // ---------------------------------------------------------------- historial

    @Test
    void historialPideLasMasRecientesYCalculaLaVariacionDeLaPrimeraContraLaAnterior() {
        PageRequest pedido = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "fecha"));
        when(cotizaciones.findAll(any(Specification.class), eq(pedido))).thenReturn(new PageImpl<>(List.of(
                cotizacion(jugador, DOMINGO_3, "0.9600", "0.1", ACTIVA),
                cotizacion(jugador, DOMINGO_2, "1.2000", "0.9", ACTIVA)), pedido, 3));
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanOrderByFechaDesc(7L, ACTIVA, DOMINGO_2))
                .thenReturn(Optional.of(cotizacion(jugador, DOMINGO_1, "1.0000", "0.5", ACTIVA)));

        HistorialCotizacionesResultado resultado = service.historial(7L, null, null, null, 0, 2, false);

        assertEquals(List.of(DOMINGO_2, DOMINGO_3), resultado.cotizaciones().stream().map(CotizacionLeida::fecha).toList());
        assertEquals(new BigDecimal("20.00"), resultado.cotizaciones().get(0).variacion());
        assertEquals(new BigDecimal("-20.00"), resultado.cotizaciones().get(1).variacion());
        assertEquals(3, resultado.totalElements());
        assertEquals(2, resultado.totalPages());
    }

    @Test
    void historialSinCotizacionAnteriorTieneLaPrimeraSinVariacion() {
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                cotizacion(jugador, DOMINGO_1, "1.0000", "0.5", ACTIVA))));
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanOrderByFechaDesc(7L, ACTIVA, DOMINGO_1))
                .thenReturn(Optional.empty());

        HistorialCotizacionesResultado resultado = service.historial(7L, null, null, DOMINGO_1, 0, 20, false);

        assertEquals(1, resultado.cotizaciones().size());
        assertNull(resultado.cotizaciones().get(0).variacion());
        assertEquals(7L, resultado.jugadorId());
    }

    @Test
    void historialRechazaPaginacionInvalida() {
        assertThrows(IllegalArgumentException.class, () -> service.historial(7L, null, null, null, -1, 20, false));
        assertThrows(IllegalArgumentException.class, () -> service.historial(7L, null, null, null, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> service.historial(7L, null, null, null, 0, 101, false));
    }

    @Test
    void historialRechazaDesdePosteriorAHasta() {
        assertThrows(IllegalArgumentException.class, () -> service.historial(7L, null, DOMINGO_3, DOMINGO_1, 0, 20, false));
    }

    @Test
    void historialDeJugadorInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.historial(99L, null, null, null, 0, 20, false));
    }

    // ---------------------------------------------------------------- vigente

    @Test
    void vigenteSinFechaUsaLaFechaDeNegocio() {
        Cotizacion actual = cotizacion(jugador, DOMINGO_3, "1.1000", "0.6", ACTIVA);
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(7L, ACTIVA, HOY_NEGOCIO))
                .thenReturn(Optional.of(actual));
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                7L, ACTIVA, DOMINGO_3.minusDays(1))).thenReturn(Optional.empty());

        CotizacionLeida leida = service.vigente(7L, null, null, false);

        assertEquals(DOMINGO_3, leida.fecha());
        assertNull(leida.variacion());
    }

    @Test
    void vigenteAUnaFechaPedida() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(7L, OTRA, jueves))
                .thenReturn(Optional.of(cotizacion(jugador, DOMINGO_1, "1.0500", "0.6", OTRA)));
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                7L, OTRA, DOMINGO_1.minusDays(1))).thenReturn(Optional.empty());

        CotizacionLeida leida = service.vigente(7L, OTRA, jueves, true);

        assertEquals(DOMINGO_1, leida.fecha());
        assertEquals(OTRA, leida.estrategia());
    }

    @Test
    void vigenteSinCotizacionesHastaEsaFechaDa404() {
        LocalDate fecha = LocalDate.of(2026, 7, 1);
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(7L, ACTIVA, fecha))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.vigente(7L, null, fecha, false));
    }

    @Test
    void vigenteDeJugadorInexistenteDa404() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.vigente(99L, null, null, false));
    }

    @Test
    void vigenteSinEstrategiaActivaDa404() {
        when(estrategias.findByActivaTrue()).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.vigente(7L, null, null, false));
    }

    // ---------------------------------------------------------------- ranking

    @Test
    @SuppressWarnings("unchecked")
    void rankingNumeraPuestosDeLaSegundaPaginaYCalculaVariaciones() {
        Jugador otro = jugador(8L, "Pedri");
        when(cotizaciones.findMaxFechaByEstrategia(ACTIVA)).thenReturn(Optional.of(DOMINGO_3));
        when(cotizaciones.findVersionByEstrategiaAndFecha(ACTIVA, DOMINGO_3)).thenReturn(Optional.of(2));
        when(cotizaciones.findFechaAnterior(ACTIVA, DOMINGO_3)).thenReturn(Optional.of(DOMINGO_2));
        List<Cotizacion> pagina = List.of(
                cotizacion(jugador, DOMINGO_3, "1.1000", "0.9", ACTIVA),
                cotizacion(otro, DOMINGO_3, "0.8000", "0.4", ACTIVA));
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(pagina, PageRequest.of(1, 2), 6));
        when(cotizaciones.findByEstrategiaAndFechaAndJugadorIdIn(eq(ACTIVA), eq(DOMINGO_2), anyCollection()))
                .thenReturn(List.of(cotizacion(jugador, DOMINGO_2, "1.0000", "0.5", ACTIVA)));

        RankingResultado ranking = service.ranking(null, "la liga", "del", 1, 2, false);

        assertEquals(ACTIVA, ranking.estrategia());
        assertEquals(2, ranking.version());
        assertEquals(DOMINGO_3, ranking.fecha());
        assertEquals(6, ranking.totalElements());
        assertEquals(3, ranking.totalPages());
        assertEquals(3, ranking.items().get(0).puesto());
        assertEquals(4, ranking.items().get(1).puesto());
        assertEquals(new BigDecimal("10.00"), ranking.items().get(0).cotizacion().variacion());
        assertNull(ranking.items().get(1).cotizacion().variacion());
        assertSame(otro, ranking.items().get(1).jugador());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(cotizaciones).findAll(any(Specification.class), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(2, pageable.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("score").getDirection());
        assertEquals(Sort.Direction.ASC, pageable.getValue().getSort().getOrderFor("jugador.id").getDirection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rankingSinSemanaAnteriorNoTieneVariaciones() {
        when(cotizaciones.findMaxFechaByEstrategia(ACTIVA)).thenReturn(Optional.of(DOMINGO_1));
        when(cotizaciones.findVersionByEstrategiaAndFecha(ACTIVA, DOMINGO_1)).thenReturn(Optional.of(1));
        when(cotizaciones.findFechaAnterior(ACTIVA, DOMINGO_1)).thenReturn(Optional.empty());
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cotizacion(jugador, DOMINGO_1, "1.0000", "0.5", ACTIVA)),
                        PageRequest.of(0, 20), 1));

        RankingResultado ranking = service.ranking(null, null, null, 0, 20, false);

        assertEquals(1, ranking.items().get(0).puesto());
        assertNull(ranking.items().get(0).cotizacion().variacion());
        verify(cotizaciones, never()).findByEstrategiaAndFechaAndJugadorIdIn(any(), any(), anyCollection());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rankingConFiltrosQueNoEncuentranNadaDevuelvePaginaVaciaConFecha() {
        when(cotizaciones.findMaxFechaByEstrategia(OTRA)).thenReturn(Optional.of(DOMINGO_3));
        when(cotizaciones.findVersionByEstrategiaAndFecha(OTRA, DOMINGO_3)).thenReturn(Optional.of(1));
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        RankingResultado ranking = service.ranking(OTRA, "Serie A", "POR", 0, 20, true);

        assertTrue(ranking.items().isEmpty());
        assertEquals(DOMINGO_3, ranking.fecha());
        verify(cotizaciones, never()).findFechaAnterior(any(), any());
    }

    @Test
    void rankingSinRecalculoDevuelvePaginaVacia() {
        when(cotizaciones.findMaxFechaByEstrategia(ACTIVA)).thenReturn(Optional.empty());

        RankingResultado ranking = service.ranking(null, null, null, 0, 20, false);
        RankingResultado repetido = service.ranking(null, null, null, 0, 20, false);

        assertEquals(ACTIVA, ranking.estrategia());
        assertNull(ranking.version());
        assertNull(ranking.fecha());
        assertEquals(0, ranking.totalElements());
        assertEquals(0, ranking.totalPages());
        assertTrue(ranking.items().isEmpty());
        assertSame(ranking, repetido);
        verify(cotizaciones, times(1)).findMaxFechaByEstrategia(ACTIVA);
    }

    @Test
    void rankingSinEstrategiaActivaDevuelvePaginaVacia() {
        when(estrategias.findByActivaTrue()).thenReturn(Optional.empty());

        RankingResultado ranking = service.ranking(null, null, null, 0, 20, false);
        RankingResultado repetido = service.ranking(null, null, null, 0, 20, false);

        assertNull(ranking.estrategia());
        assertTrue(ranking.items().isEmpty());
        assertSame(ranking, repetido);
        verify(estrategias, times(2)).findByActivaTrue();
        verifyNoInteractions(cotizaciones);
    }

    @Test
    @SuppressWarnings("unchecked")
    void rankingRepetidoNoVuelveAConsultarCotizacionesPeroResuelveLaActiva() {
        when(cotizaciones.findMaxFechaByEstrategia(ACTIVA)).thenReturn(Optional.of(DOMINGO_1));
        when(cotizaciones.findVersionByEstrategiaAndFecha(ACTIVA, DOMINGO_1)).thenReturn(Optional.of(1));
        when(cotizaciones.findFechaAnterior(ACTIVA, DOMINGO_1)).thenReturn(Optional.empty());
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cotizacion(jugador, DOMINGO_1, "1.0000", "0.5", ACTIVA)),
                        PageRequest.of(0, 20), 1));

        RankingResultado primero = service.ranking(null, null, null, 0, 20, false);
        RankingResultado segundo = service.ranking(null, null, null, 0, 20, false);

        assertSame(primero, segundo);
        verify(estrategias, times(2)).findByActivaTrue();
        verify(cotizaciones, times(1)).findMaxFechaByEstrategia(ACTIVA);
        verify(cotizaciones, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(cotizaciones, times(1)).findFechaAnterior(ACTIVA, DOMINGO_1);
        verify(cotizaciones, times(1)).findVersionByEstrategiaAndFecha(ACTIVA, DOMINGO_1);
    }

    @Test
    void rankingClavesNormalizadasCompartenEntradaYClavesDistintasNo() {
        when(cotizaciones.findMaxFechaByEstrategia(anyString())).thenReturn(Optional.empty());

        RankingResultado normalizado = service.ranking(null, "La Liga", "del", 0, 10, false);
        assertSame(normalizado, service.ranking(null, "la liga", "DEL", 0, 10, false));

        service.ranking(null, "Serie A", "DEL", 0, 10, false);
        service.ranking(null, "La Liga", "POR", 0, 10, false);
        service.ranking(null, "La Liga", "DEL", 1, 10, false);
        service.ranking(null, "La Liga", "DEL", 0, 11, false);
        service.ranking(OTRA, null, null, 0, 10, true);

        verify(cotizaciones, times(5)).findMaxFechaByEstrategia(ACTIVA);
        verify(cotizaciones, times(1)).findMaxFechaByEstrategia(OTRA);
    }

    @Test
    void rankingVacioSeRecargaDespuesDeInvalidar() {
        Cache<String, CotizacionVigentePort.CotizacionVigente> cotizacionCache = Caffeine.newBuilder().build();
        CotizacionCacheInvalidator invalidator = new CotizacionCacheInvalidator(cotizacionCache, rankingCache);
        when(cotizaciones.findMaxFechaByEstrategia(ACTIVA))
                .thenReturn(Optional.empty(), Optional.of(DOMINGO_1));
        when(cotizaciones.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        RankingResultado vacio = service.ranking(null, null, null, 0, 20, false);
        assertSame(vacio, service.ranking(null, null, null, 0, 20, false));

        invalidator.invalidate();

        RankingResultado actualizado = service.ranking(null, null, null, 0, 20, false);
        assertNull(vacio.fecha());
        assertEquals(DOMINGO_1, actualizado.fecha());
        verify(cotizaciones, times(2)).findMaxFechaByEstrategia(ACTIVA);
    }

    @Test
    void rankingCacheadoDeOtraEstrategiaNoEludeElPermiso() {
        when(cotizaciones.findMaxFechaByEstrategia(OTRA)).thenReturn(Optional.empty());
        RankingResultado cacheado = service.ranking(OTRA, null, null, 0, 20, true);

        assertSame(cacheado, rankingCache.getIfPresent(
                new RankingClave(OTRA, null, null, 0, 20)));
        assertThrows(SoloSuperusuarioException.class,
                () -> service.ranking(OTRA, null, null, 0, 20, false));

        verify(estrategias, times(2)).findByActivaTrue();
        verify(cotizaciones, times(1)).findMaxFechaByEstrategia(OTRA);
    }

    @Test
    void rankingValidaFiltrosYPaginacion() {
        assertThrows(IllegalArgumentException.class, () -> service.ranking(null, null, null, -1, 20, false));
        assertThrows(IllegalArgumentException.class, () -> service.ranking(null, null, null, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> service.ranking(null, null, null, 0, 101, false));
        assertThrows(IllegalArgumentException.class, () -> service.ranking(null, "Liga Argentina", null, 0, 20, false));
        assertThrows(IllegalArgumentException.class, () -> service.ranking(null, null, "ARQ", 0, 20, false));
    }

    @Test
    void rankingDeOtraEstrategiaParaUnUsuarioDa403() {
        assertThrows(SoloSuperusuarioException.class, () -> service.ranking(OTRA, null, null, 0, 20, false));
    }
}
