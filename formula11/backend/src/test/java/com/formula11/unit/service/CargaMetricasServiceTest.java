package com.formula11.unit.service;

import com.formula11.adapter.DatosPartidoWhoScored;
import com.formula11.adapter.PartidoWhoScoredResumen;
import com.formula11.adapter.WhoScoredPort;
import com.formula11.config.ClockConfig;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.EjecucionMetricaLiga;
import com.formula11.model.EjecucionMetricas;
import com.formula11.model.Liga;
import com.formula11.persistence.EjecucionMetricaLigaRepository;
import com.formula11.persistence.EjecucionMetricasRepository;
import com.formula11.persistence.PendienteVinculacionRepository;
import com.formula11.service.CargaMetricasService;
import com.formula11.service.CargaPartidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CargaMetricasServiceTest {
    private static final Instant AHORA = Instant.parse("2026-10-28T15:00:00Z");
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private WhoScoredPort port;
    private CargaPartidoService partidos;
    private EjecucionMetricasRepository ejecuciones;
    private EjecucionMetricaLigaRepository ligas;
    private ObjectProvider<CargaMetricasService> self;
    private CargaMetricasService service;
    private EjecucionMetricas guardada;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        port = mock(WhoScoredPort.class);
        partidos = mock(CargaPartidoService.class);
        ejecuciones = mock(EjecucionMetricasRepository.class);
        ligas = mock(EjecucionMetricaLigaRepository.class);
        self = mock(ObjectProvider.class);
        when(ejecuciones.save(any(EjecucionMetricas.class))).thenAnswer(invocation -> {
            EjecucionMetricas ejecucion = invocation.getArgument(0);
            if (guardada == null) {
                guardada = ejecucion;
            }
            return ejecucion;
        });
        when(ejecuciones.findById(any())).thenAnswer(invocation -> Optional.ofNullable(guardada));
        when(ligas.save(any(EjecucionMetricaLiga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new CargaMetricasService(port, new WhoScoredProperties(), partidos, ejecuciones, ligas,
                mock(PendienteVinculacionRepository.class), Clock.fixed(AHORA, ZoneOffset.UTC), self);
    }

    private static PartidoWhoScoredResumen partido(long id, String elapsed) {
        return new PartidoWhoScoredResumen(id, ZonedDateTime.of(2026, 10, 9, 16, 0, 0, 0,
                ClockConfig.ZONA_NEGOCIO), "Local", "Visitante", elapsed, Liga.LA_LIGA);
    }

    @Test
    void iniciarCreaLaEjecucionYDelegaEnLaCargaAsincronica() {
        CargaMetricasService proxy = mock(CargaMetricasService.class);
        when(self.getObject()).thenReturn(proxy);

        EjecucionMetricas ejecucion = service.iniciar(null);

        assertEquals("EN_CURSO", ejecucion.getEstado());
        // Sin fecha: carga incremental desde el primer día del mes anterior.
        verify(proxy).ejecutarAsync(any(), eq(LocalDate.of(2026, 9, 1)), any());
        assertTrue(service.estaEnCurso());
    }

    @Test
    void iniciarRechazaUnaSegundaCargaMientrasHayUnaEnCurso() {
        when(self.getObject()).thenReturn(mock(CargaMetricasService.class));
        service.iniciar(null);

        LocalDate desde = LocalDate.of(2026, 10, 1);

        assertThrows(IllegalStateException.class, () -> service.iniciar(desde));
    }

    @Test
    void iniciarLiberaElLockSiFallaAntesDeLanzarLaCarga() {
        when(self.getObject()).thenThrow(new IllegalStateException("sin proxy"));

        assertThrows(IllegalStateException.class, () -> service.iniciar(null));

        assertFalse(service.estaEnCurso());
    }

    @Test
    void cargaSoloPartidosTerminadosQueNoEstabanCargados() {
        when(port.listarPartidos(Liga.LA_LIGA, OCTUBRE)).thenReturn(List.of(
                partido(1L, "FT"), partido(2L, "FT"), partido(3L, "-")));
        when(partidos.yaCargado(2L)).thenReturn(true);
        DatosPartidoWhoScored datos = new DatosPartidoWhoScored(1L, Liga.LA_LIGA, null, "{}", "{}");
        when(port.obtenerDatos(1L)).thenReturn(datos);
        when(partidos.prepararYGuardar(any(), eq(datos)))
                .thenReturn(new CargaPartidoService.ResultadoPartido(true, 3, 1));

        EjecucionMetricas ejecucion = service.cargarSincronico(LocalDate.of(2026, 10, 1));

        assertEquals("COMPLETADA", ejecucion.getEstado());
        verify(port).obtenerDatos(1L);
        verify(port, never()).obtenerDatos(2L);
        verify(port, never()).obtenerDatos(3L);
        verify(port).finalizarCarga();
        EjecucionMetricaLiga laLiga = resumenDe(Liga.LA_LIGA);
        assertEquals(3, laLiga.getPartidosEncontrados());
        assertEquals(1, laLiga.getPartidosCargados());
        assertEquals(3, laLiga.getJugadoresVinculados());
        assertEquals(1, laLiga.getPendientes());
        assertFalse(service.estaEnCurso());
    }

    @Test
    void siFallaUnaLigaSigueConLasDemasYQuedaParcial() {
        when(port.listarPartidos(eq(Liga.PREMIER_LEAGUE), any())).thenThrow(new IllegalStateException("bloqueado"));

        EjecucionMetricas ejecucion = service.cargarSincronico(LocalDate.of(2026, 10, 1));

        assertEquals("PARCIAL", ejecucion.getEstado());
        verify(port).listarPartidos(Liga.LIGUE_1, OCTUBRE);
        assertEquals(1, resumenDe(Liga.PREMIER_LEAGUE).getPartidosFallidos());
    }

    @Test
    void siFallaUnPartidoSigueConLosDemas() {
        when(port.listarPartidos(Liga.LA_LIGA, OCTUBRE)).thenReturn(List.of(partido(1L, "FT"), partido(4L, "FT")));
        when(port.obtenerDatos(1L)).thenThrow(new IllegalStateException("timeout"));
        DatosPartidoWhoScored datos = new DatosPartidoWhoScored(4L, Liga.LA_LIGA, null, "{}", "{}");
        when(port.obtenerDatos(4L)).thenReturn(datos);
        when(partidos.prepararYGuardar(any(), eq(datos)))
                .thenReturn(new CargaPartidoService.ResultadoPartido(true, 0, 0));

        EjecucionMetricas ejecucion = service.cargarSincronico(LocalDate.of(2026, 10, 1));

        assertEquals("PARCIAL", ejecucion.getEstado());
        EjecucionMetricaLiga laLiga = resumenDe(Liga.LA_LIGA);
        assertEquals(1, laLiga.getPartidosFallidos());
        assertEquals(1, laLiga.getPartidosCargados());
    }

    @Test
    void laCargaIncrementalRecorreElMesAnteriorYElActual() {

        service.actualizarIncremental();

        verify(port).listarPartidos(Liga.LA_LIGA, YearMonth.of(2026, 9));
        verify(port).listarPartidos(Liga.LA_LIGA, OCTUBRE);
        verify(port, never()).listarPartidos(Liga.LA_LIGA, YearMonth.of(2026, 8));
    }

    @Test
    void nuncaCargaAntesDelInicioDeLaTemporada() {

        service.cargarSincronico(LocalDate.of(1900, 1, 1));

        verify(port).listarPartidos(Liga.LA_LIGA, YearMonth.of(2026, 8));
        verify(port, never()).listarPartidos(Liga.LA_LIGA, YearMonth.of(2026, 7));
    }

    @Test
    void laCargaSincronicaNoArrancaSiHayOtraEnCurso() {
        when(self.getObject()).thenReturn(mock(CargaMetricasService.class));
        service.iniciar(null);

        assertNull(service.cargarSincronico(null));
        verifyNoInteractions(port);
    }

    @Test
    void marcaComoFallidasLasEjecucionesQueQuedaronEnCurso() {
        EjecucionMetricas colgada = new EjecucionMetricas("EN_CURSO", AHORA, "corr");
        when(ejecuciones.findByEstado("EN_CURSO")).thenReturn(List.of(colgada));

        service.marcarEnCursoComoFallidas();

        assertEquals("FALLIDA", colgada.getEstado());
        verify(ejecuciones).save(colgada);
    }

    private EjecucionMetricaLiga resumenDe(Liga liga) {
        ArgumentCaptor<EjecucionMetricaLiga> captor = ArgumentCaptor.forClass(EjecucionMetricaLiga.class);
        verify(ligas, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(resumen -> resumen.getLiga() == liga)
                .reduce((primero, ultimo) -> ultimo)
                .orElseThrow();
    }
}
