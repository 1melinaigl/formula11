package com.formula11.unit.service;

import com.formula11.config.SenalesProperties;
import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Senal;
import com.formula11.model.senales.MedidorAciertos;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.persistence.MetricaPartidoRepository;
import com.formula11.persistence.SenalRepository;
import com.formula11.service.PaginaSenales;
import com.formula11.service.SenalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class SenalServiceTest {
    private static final String ACTIVA = "RENDIMIENTO_POSICION";
    private static final String OTRA = "CONSISTENCIA";
    private static final LocalDate DOMINGO = LocalDate.of(2026, 10, 25);
    private static final Instant AHORA = Instant.parse("2026-10-26T09:00:00Z");

    private SenalRepository senales;
    private CotizacionRepository cotizaciones;
    private MetricaPartidoRepository metricas;
    private EstrategiaRepository estrategias;
    private SenalService service;

    @BeforeEach
    void setUp() {
        senales = mock(SenalRepository.class);
        cotizaciones = mock(CotizacionRepository.class);
        metricas = mock(MetricaPartidoRepository.class);
        estrategias = mock(EstrategiaRepository.class);
        SenalesProperties properties = new SenalesProperties(new BigDecimal("15"), new BigDecimal("0.05"), 5, 20);
        service = new SenalService(senales, cotizaciones, metricas, estrategias, properties, Clock.fixed(AHORA, ZoneOffset.UTC));
        Estrategia activa = mock(Estrategia.class);
        when(activa.getCodigo()).thenReturn(ACTIVA);
        when(estrategias.findByActivaTrue()).thenReturn(Optional.of(activa));
    }

    private static Jugador jugador(long id) {
        Jugador jugador = mock(Jugador.class);
        when(jugador.getId()).thenReturn(id);
        when(jugador.getPosicion()).thenReturn("DEL");
        when(jugador.getNombre()).thenReturn("Jugador " + id);
        when(jugador.getEquipo()).thenReturn("Equipo");
        when(jugador.getLiga()).thenReturn(Liga.SERIE_A);
        return jugador;
    }

    private static Cotizacion cotizacion(Jugador jugador, String precio) {
        return new Cotizacion(jugador, DOMINGO, new BigDecimal(precio), new BigDecimal("0.5"), ACTIVA, 1, Map.of());
    }

    private static Object[] fila(long id, String precio) {
        return new Object[]{id, new BigDecimal(precio)};
    }

    @Test
    @SuppressWarnings("unchecked")
    void generaActualizaCreaYBorraSenalesDeLaSemana() {
        Jugador conservada = jugador(1L);
        Jugador nueva = jugador(2L);
        Jugador sinJugar = jugador(3L);
        Jugador yaNoAplica = jugador(4L);
        when(cotizaciones.findByEstrategiaAndFecha(ACTIVA, DOMINGO)).thenReturn(List.of(
                cotizacion(conservada, "1.0000"), cotizacion(nueva, "1.0000"), cotizacion(sinJugar, "1.0000")));
        when(cotizaciones.findPreciosByEstrategiaAndFecha(OTRA, DOMINGO)).thenReturn(List.of(
                fila(1L, "1.5000"), fila(2L, "1.2000"), fila(3L, "2.0000")));
        when(metricas.findJugadoresQueJugaron(DOMINGO.minusDays(6), DOMINGO)).thenReturn(List.of(1L, 2L));
        Senal existente = new Senal(conservada, DOMINGO, Senal.Tipo.ENTRE_ESTRATEGIAS, OTRA,
                new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("1.1"), AHORA);
        Senal vieja = new Senal(yaNoAplica, DOMINGO, Senal.Tipo.ENTRE_ESTRATEGIAS, ACTIVA,
                new BigDecimal("30"), new BigDecimal("1"), new BigDecimal("1.3"), AHORA);
        when(senales.findByFecha(DOMINGO)).thenReturn(List.of(existente, vieja));

        service.generar(DOMINGO);

        ArgumentCaptor<Collection<Senal>> borradas = ArgumentCaptor.forClass(Collection.class);
        verify(senales).deleteAll(borradas.capture());
        assertEquals(List.of(vieja), new ArrayList<>(borradas.getValue()));
        ArgumentCaptor<List<Senal>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(senales).saveAll(guardadas.capture());
        assertEquals(2, guardadas.getValue().size());
        assertSame(existente, guardadas.getValue().get(0));
        assertEquals(ACTIVA, existente.getEstrategia());
        assertEquals(new BigDecimal("50.00"), existente.getBrecha());
        Senal creada = guardadas.getValue().get(1);
        assertSame(nueva, creada.getJugador());
        assertEquals(new BigDecimal("20.00"), creada.getBrecha());
        assertEquals(AHORA, creada.getCreadaEn());
    }

    @Test
    void sinEstrategiaActivaNoGenera() {
        when(estrategias.findByActivaTrue()).thenReturn(Optional.empty());

        service.generar(DOMINGO);

        verifyNoInteractions(cotizaciones, senales);
    }

    @Test
    void conConsistenciaActivaComparaContraRendimiento() {
        Estrategia consistencia = mock(Estrategia.class);
        when(consistencia.getCodigo()).thenReturn(OTRA);
        when(estrategias.findByActivaTrue()).thenReturn(Optional.of(consistencia));
        when(cotizaciones.findByEstrategiaAndFecha(OTRA, DOMINGO)).thenReturn(List.of());
        when(senales.findByFecha(DOMINGO)).thenReturn(List.of());

        service.generar(DOMINGO);

        verify(cotizaciones).findPreciosByEstrategiaAndFecha(ACTIVA, DOMINGO);
        verify(senales).saveAll(anyList());
    }

    @Test
    void listaLasVigentesDeLaUltimaFecha() {
        Jugador jugador = jugador(1L);
        Senal senal = new Senal(jugador, DOMINGO, Senal.Tipo.PRECIO_ATRASADO, ACTIVA,
                new BigDecimal("18"), new BigDecimal("1"), new BigDecimal("1.18"), AHORA);
        when(senales.findMaxFecha()).thenReturn(Optional.of(DOMINGO));
        when(senales.findByFecha(eqFecha(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(senal), PageRequest.of(0, 20), 1));

        PaginaSenales pagina = service.listar(true, 0, 20);

        assertEquals(1, pagina.totalElements());
        assertEquals(1, pagina.totalPages());
        assertSame(jugador, pagina.content().get(0).jugador());
        assertEquals(Senal.Tipo.PRECIO_ATRASADO, pagina.content().get(0).tipo());
        assertEquals(new BigDecimal("18.00"), pagina.content().get(0).brecha());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(senales).findByFecha(eqFecha(), pageable.capture());
        assertNotNull(pageable.getValue().getSort().getOrderFor("brecha"));
    }

    private static LocalDate eqFecha() {
        return org.mockito.ArgumentMatchers.eq(DOMINGO);
    }

    @Test
    void sinSenalesDevuelvePaginaVacia() {
        when(senales.findMaxFecha()).thenReturn(Optional.empty());

        PaginaSenales pagina = service.listar(true, 0, 20);

        assertTrue(pagina.content().isEmpty());
        assertEquals(0, pagina.totalElements());
    }

    @Test
    void conVigentesFalseListaTodas() {
        when(senales.findAllBy(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 5));

        PaginaSenales pagina = service.listar(false, 1, 5);

        assertEquals(5, pagina.totalElements());
        assertEquals(1, pagina.page());
        verify(senales, never()).findMaxFecha();
    }

    @Test
    void validaLaPaginacion() {
        assertThrows(IllegalArgumentException.class, () -> service.listar(true, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.listar(true, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.listar(true, 0, 101));
    }

    @Test
    void midiendoAciertosSalteaLasSemanasSinSiguiente() {
        Jugador jugador = jugador(1L);
        LocalDate anterior = DOMINGO.minusWeeks(1);
        Senal medible = new Senal(jugador, anterior, Senal.Tipo.ENTRE_ESTRATEGIAS, ACTIVA,
                new BigDecimal("20"), new BigDecimal("1"), new BigDecimal("1.2"), AHORA);
        Senal ultima = new Senal(jugador, DOMINGO, Senal.Tipo.ENTRE_ESTRATEGIAS, ACTIVA,
                new BigDecimal("20"), new BigDecimal("1"), new BigDecimal("1.2"), AHORA);
        when(senales.findAll()).thenReturn(List.of(ultima, medible));
        when(cotizaciones.findPreciosByEstrategiaAndFecha(ACTIVA, anterior))
                .thenReturn(List.of(fila(1L, "1.0000"), fila(2L, "1.0000")));
        when(cotizaciones.findPreciosByEstrategiaAndFecha(ACTIVA, DOMINGO))
                .thenReturn(List.of(fila(1L, "1.2000"), fila(2L, "1.0000")));
        when(cotizaciones.findPreciosByEstrategiaAndFecha(ACTIVA, DOMINGO.plusWeeks(1))).thenReturn(List.of());

        MedidorAciertos.Resultado resultado = service.aciertos();

        assertEquals(1, resultado.semanas().size());
        assertEquals(anterior, resultado.semanas().get(0).fecha());
        assertEquals(1, resultado.semanas().get(0).aciertos());
        assertEquals(new BigDecimal("100.00"), resultado.tasaAcierto());
        assertEquals(new BigDecimal("10.00"), resultado.diferenciaPromedio());
    }
}
