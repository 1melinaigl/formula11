package com.formula11.unit.service;

import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.service.CotizacionHistoricaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CotizacionHistoricaServiceTest {
    private static final String ACTIVA = "CONSISTENCIA";
    private static final LocalDate DESDE = LocalDate.of(2026, 9, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 30);
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 17);

    private CotizacionRepository cotizaciones;
    private EstrategiaRepository estrategias;
    private CotizacionHistoricaService service;

    @BeforeEach
    void setUp() {
        cotizaciones = mock(CotizacionRepository.class);
        estrategias = mock(EstrategiaRepository.class);
        service = new CotizacionHistoricaService(cotizaciones, estrategias);
        Estrategia activa = mock(Estrategia.class);
        when(activa.getCodigo()).thenReturn(ACTIVA);
        when(estrategias.findByActivaTrue()).thenReturn(Optional.of(activa));
    }

    private static Cotizacion cotizacion(long jugadorId, String precio) {
        Jugador jugador = mock(Jugador.class);
        when(jugador.getId()).thenReturn(jugadorId);
        return new Cotizacion(jugador, LocalDate.of(2026, 9, 13), new BigDecimal(precio),
                new BigDecimal("0.5"), ACTIVA, 1, Map.of());
    }

    @Test
    void devuelveLasFechasDeLaEstrategiaActiva() {
        List<LocalDate> domingos = List.of(LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 13));
        when(cotizaciones.findFechasByEstrategiaBetween(ACTIVA, DESDE, HASTA)).thenReturn(domingos);

        assertEquals(domingos, service.fechasCotizacion(DESDE, HASTA));
    }

    @Test
    void fechasVaciasConNulosRangoInvertidoOSinActiva() {
        assertTrue(service.fechasCotizacion(null, HASTA).isEmpty());
        assertTrue(service.fechasCotizacion(DESDE, null).isEmpty());
        assertTrue(service.fechasCotizacion(HASTA, DESDE).isEmpty());

        when(estrategias.findByActivaTrue()).thenReturn(Optional.empty());
        assertTrue(service.fechasCotizacion(DESDE, HASTA).isEmpty());
        verify(cotizaciones, never()).findFechasByEstrategiaBetween(any(), any(), any());
    }

    @Test
    void devuelveUnPrecioPorJugadorYUnoParaLosQueNoTienenCotizacion() {
        Cotizacion conPrecio = cotizacion(1L, "1.2345");
        when(cotizaciones.findVigentesA(eq(ACTIVA), anyCollection(), eq(FECHA))).thenReturn(List.of(conPrecio));

        Map<Long, BigDecimal> precios = service.preciosA(List.of(1L, 2L, 2L), FECHA);

        assertEquals(2, precios.size());
        assertEquals(new BigDecimal("1.2345"), precios.get(1L));
        assertEquals(new BigDecimal("1.0000"), precios.get(2L));
        verify(cotizaciones).findVigentesA(ACTIVA, Set.of(1L, 2L), FECHA);
    }

    @Test
    void preciosVaciosConColeccionNulaOVacia() {
        assertTrue(service.preciosA(null, FECHA).isEmpty());
        assertTrue(service.preciosA(List.of(), FECHA).isEmpty());
        verifyNoInteractions(cotizaciones);
    }

    @Test
    void sinEstrategiaActivaTodosValenUno() {
        when(estrategias.findByActivaTrue()).thenReturn(Optional.empty());

        Map<Long, BigDecimal> precios = service.preciosA(List.of(3L), FECHA);

        assertEquals(Map.of(3L, new BigDecimal("1.0000")), precios);
        verify(cotizaciones, never()).findVigentesA(any(), anyCollection(), any());
    }

    @Test
    void ignoraIdsNulosYFechaNula() {
        Map<Long, BigDecimal> precios = service.preciosA(Arrays.asList(4L, null), null);

        assertEquals(Map.of(4L, new BigDecimal("1.0000")), precios);
        verify(cotizaciones, never()).findVigentesA(any(), anyCollection(), any());
    }

    @Test
    void soloIdsNulosDevuelveMapaVacio() {
        assertTrue(service.preciosA(Arrays.asList((Long) null), FECHA).isEmpty());
        verifyNoInteractions(cotizaciones);
    }
}
