package com.formula11.unit.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.service.CotizacionVigentePort;
import com.formula11.service.CotizacionVigenteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CotizacionVigenteServiceTest {
    @Mock CotizacionRepository cotizaciones;
    @Mock EstrategiaRepository estrategias;

    private final ZoneId zona = ZoneId.of("America/Argentina/Buenos_Aires");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-25T02:00:00Z"), zona);

    @Test
    void convierteInstantConLaZonaYBuscaLaCotizacionVigente() {
        Jugador jugador = mockJugador(7L);
        Estrategia activa = estrategiaActiva();
        Cotizacion cotizacion = cotizacion(jugador, LocalDate.of(2026, 10, 24), "RENDIMIENTO_POSICION");
        when(estrategias.findByActivaTrue()).thenReturn(Optional.of(activa));
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                7L, "RENDIMIENTO_POSICION", LocalDate.of(2026, 10, 24))).thenReturn(Optional.of(cotizacion));

        var result = servicio(Caffeine.newBuilder().build()).vigente(jugador,
                Instant.parse("2026-10-25T02:30:00Z"));

        assertEquals(new CotizacionVigentePort.Cotizacion(
                new BigDecimal("1.2345"), "RENDIMIENTO_POSICION", 1,
                LocalDate.of(2026, 10, 24)), result);
    }

    @Test
    void devuelvePrecioInicialConMetadatosNulosCuandoNoHayCotizacion() {
        Jugador jugador = mockJugador(8L);
        when(estrategias.findByActivaTrue()).thenReturn(Optional.empty());

        var result = servicio(Caffeine.newBuilder().build()).vigente(jugador, clock.instant());

        assertEquals(new CotizacionVigentePort.Cotizacion(
                BigDecimal.ONE, null, null, null), result);
    }

    @Test
    void usaCacheYNoConsultaDosVeces() {
        Jugador jugador = mockJugador(9L);
        Estrategia activa = estrategiaActiva();
        when(estrategias.findByActivaTrue()).thenReturn(Optional.of(activa));
        when(cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                9L, "RENDIMIENTO_POSICION", LocalDate.of(2026, 10, 24))).thenReturn(Optional.empty());
        Cache<String, CotizacionVigentePort.Cotizacion> cache = Caffeine.newBuilder().build();
        var service = servicio(cache);

        var first = service.vigente(jugador, clock.instant());
        var second = service.vigente(jugador, clock.instant());

        assertSame(first, second);
        verify(estrategias, times(1)).findByActivaTrue();
        verify(cotizaciones, times(1))
                .findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                        9L, "RENDIMIENTO_POSICION", LocalDate.of(2026, 10, 24));
    }

    private CotizacionVigenteService servicio(Cache<String, CotizacionVigentePort.Cotizacion> cache) {
        return new CotizacionVigenteService(cotizaciones, estrategias, clock, cache);
    }

    private static Jugador mockJugador(Long id) {
        Jugador jugador = mock(Jugador.class);
        when(jugador.getId()).thenReturn(id);
        return jugador;
    }

    private static Estrategia estrategiaActiva() {
        return new Estrategia("RENDIMIENTO_POSICION", "Rendimiento", 1,
                Map.of("DEL", Map.of("goles", 1d)),
                Map.of("k", 0.4d, "topeVariacion", 0.2d, "piso", 0.1d),
                true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static Cotizacion cotizacion(Jugador jugador, LocalDate fecha, String estrategia) {
        return new Cotizacion(jugador, fecha, new BigDecimal("1.2345"), new BigDecimal("0.75"),
                estrategia, 1, Map.of("DEL", Map.of("goles", 1d)));
    }
}
