package com.formula11.unit.service;

import com.formula11.model.*;
import com.formula11.persistence.*;
import com.formula11.service.RecalculoSemanaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RecalculoSemanaServiceTest {
    @Mock JugadorRepository jugadores;
    @Mock MetricaPartidoRepository metricas;
    @Mock EstrategiaRepository estrategias;
    @Mock CotizacionRepository cotizaciones;

    @Test
    void semanaSinPartidosConservaPrecioAnteriorYJugadorSinHistorialQuedaEnUno() {
        Jugador jugador = jugador(1L);
        Estrategia estrategia = estrategia();
        when(jugadores.findAll()).thenReturn(List.of(jugador));
        when(metricas.findByFechaPartidoBetween(any(), any())).thenReturn(List.of());
        when(estrategias.findAll()).thenReturn(List.of(estrategia));
        when(cotizaciones.findByFechaLessThanOrderByFechaDesc(LocalDate.of(2026, 1, 4)))
                .thenReturn(List.of(cotizacion(jugador, new BigDecimal("1.2345"), LocalDate.of(2025, 12, 28))));

        new RecalculoSemanaService(jugadores, metricas, estrategias, cotizaciones)
                .procesar(LocalDate.of(2026, 1, 4));

        verify(cotizaciones).deleteByFecha(LocalDate.of(2026, 1, 4));
        verify(cotizaciones).saveAll(argThat(cotizaciones -> {
            Cotizacion cotizacion = cotizaciones.iterator().next();
            return cotizacion.getPrecio().compareTo(new BigDecimal("1.2345")) == 0
                    && cotizacion.getScore().compareTo(BigDecimal.ZERO) == 0;
        }));
    }

    @Test
    void jugadorQueJuegaAplicaFormulaSobrePrecioAnteriorOPrecioInicial() {
        Jugador primero = jugador(1L);
        Jugador segundo = jugador(2L);
        Estrategia estrategia = estrategia();
        when(jugadores.findAll()).thenReturn(List.of(primero, segundo));
        MetricaPartido metricaPrimero = metrica(primero, BigDecimal.valueOf(8));
        MetricaPartido metricaSegundo = metrica(segundo, BigDecimal.valueOf(6));
        when(metricas.findByFechaPartidoBetween(any(), any()))
                .thenReturn(List.of(metricaPrimero, metricaSegundo));
        when(estrategias.findAll()).thenReturn(List.of(estrategia));
        when(cotizaciones.findByFechaLessThanOrderByFechaDesc(LocalDate.of(2026, 1, 4)))
                .thenReturn(List.of());

        new RecalculoSemanaService(jugadores, metricas, estrategias, cotizaciones)
                .procesar(LocalDate.of(2026, 1, 4));

        verify(cotizaciones).saveAll(argThat(cotizaciones -> {
            List<Cotizacion> nuevas = new ArrayList<>();
            cotizaciones.forEach(nuevas::add);
            return nuevas.size() == 2
                    && nuevas.stream().anyMatch(c -> c.getJugador().getId().equals(1L)
                    && c.getPrecio().compareTo(new BigDecimal("1.1000")) == 0)
                    && nuevas.stream().anyMatch(c -> c.getJugador().getId().equals(2L)
                    && c.getPrecio().compareTo(new BigDecimal("0.9000")) == 0);
        }));
    }

    @Test
    void procesaCadaSemanaEnUnaTransaccionPropia() throws Exception {
        assertEquals(Transactional.class,
                RecalculoSemanaService.class.getMethod("procesar", LocalDate.class)
                        .getAnnotation(Transactional.class).annotationType());
    }

    private static Jugador jugador(long id) {
        Jugador jugador = mock(Jugador.class);
        when(jugador.getId()).thenReturn(id);
        lenient().when(jugador.getPosicion()).thenReturn("DEL");
        lenient().when(jugador.getLiga()).thenReturn(Liga.LA_LIGA);
        return jugador;
    }

    private static MetricaPartido metrica(Jugador jugador, BigDecimal rating) {
        MetricaPartido metrica = mock(MetricaPartido.class);
        when(metrica.getJugador()).thenReturn(jugador);
        when(metrica.getFechaPartido()).thenReturn(LocalDate.of(2026, 1, 1));
        when(metrica.getMinutos()).thenReturn(90);
        when(metrica.getTitular()).thenReturn(true);
        when(metrica.getRating()).thenReturn(rating);
        return metrica;
    }

    private static Estrategia estrategia() {
        return new Estrategia("RENDIMIENTO_POSICION", "Rendimiento", 1,
                Map.of("DEL", Map.of("rating", 1d)),
                Map.of("k", 0.4d, "topeVariacion", 0.2d, "piso", 0.1d),
                true, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static Cotizacion cotizacion(Jugador jugador, BigDecimal precio, LocalDate fecha) {
        return new Cotizacion(jugador, fecha, precio, BigDecimal.ZERO, "RENDIMIENTO_POSICION",
                1, Map.of("DEL", Map.of("rating", 1d)));
    }
}
