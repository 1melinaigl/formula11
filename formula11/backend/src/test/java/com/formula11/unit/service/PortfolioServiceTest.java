package com.formula11.unit.service;

import com.formula11.model.*;
import com.formula11.persistence.*;
import com.formula11.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// El @BeforeEach prepara al usuario y su cuenta; los casos de acceso denegado cortan antes de usarlos.
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PortfolioServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock CuentaRepository cuentas;
    @Mock TenenciaRepository tenencias;
    @Mock TransaccionRepository transacciones;
    @Mock AuditoriaMercadoRepository auditorias;
    @Mock CotizacionVigentePort cotizaciones;
    @Mock CotizacionHistoricaPort cotizacionesHistoricas;

    private PortfolioService service;
    private Usuario usuario;
    private Jugador jugador;
    private Instant ahora;

    @BeforeEach
    void preparar() {
        ahora = Instant.parse("2026-10-25T22:30:15Z");
        service = new PortfolioService(
                usuarios, cuentas, tenencias, transacciones, auditorias, cotizaciones, cotizacionesHistoricas,
                Clock.fixed(ahora, ZoneOffset.UTC));
        usuario = new Usuario("Ana", "ana-portfolio@example.com", "hash", ahora);
        jugador = new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero");
        ReflectionTestUtils.setField(usuario, "id", 1L);
        ReflectionTestUtils.setField(jugador, "id", 3L);
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
        when(cuentas.findByUsuarioId(1L)).thenReturn(Optional.of(new Cuenta(usuario, new BigDecimal("100"))));
    }

    @Test
    void calculaPortfolioConPrecisionYFechaDelClock() {
        Tenencia tenencia = new Tenencia(usuario, jugador, 3, new BigDecimal("2.55555"));
        when(tenencias.findByUsuarioIdOrderByJugadorIdAsc(1L)).thenReturn(List.of(tenencia));
        when(cotizaciones.vigente(jugador, ahora)).thenReturn(
                new CotizacionVigentePort.CotizacionVigente(new BigDecimal("3.33335"), "activa", 1, null));

        PortfolioResultado resultado = service.portfolio(1L, 1L);

        assertEquals(java.time.LocalDate.of(2026, 10, 25), resultado.fechaValuacion());
        assertEquals(new BigDecimal("7.6668"), resultado.invertido());
        assertEquals(new BigDecimal("10.0002"), resultado.valorTenencias());
        assertEquals(new BigDecimal("2.3334"), resultado.ganancia());
        assertEquals(new BigDecimal("30.44"), resultado.gananciaPorcentaje());
        assertEquals(1, resultado.posiciones().size());
        verify(cotizaciones).vigente(jugador, ahora);
    }

    @Test
    void excluyeTenenciasCeroYDevuelveCeroCuandoNoHayInvertido() {
        Tenencia cero = new Tenencia(usuario, jugador, 0, BigDecimal.TEN);
        when(tenencias.findByUsuarioIdOrderByJugadorIdAsc(1L)).thenReturn(List.of(cero));

        PortfolioResultado resultado = service.portfolio(1L, 1L);

        assertTrue(resultado.posiciones().isEmpty());
        assertEquals(new BigDecimal("0.0000"), resultado.invertido());
        assertEquals(new BigDecimal("0.0000"), resultado.ganancia());
        assertEquals(new BigDecimal("0.00"), resultado.gananciaPorcentaje());
        verifyNoInteractions(cotizaciones);
    }

    @Test
    void conservaGananciaNegativa() {
        Tenencia tenencia = new Tenencia(usuario, jugador, 2, new BigDecimal("10"));
        when(tenencias.findByUsuarioIdOrderByJugadorIdAsc(1L)).thenReturn(List.of(tenencia));
        when(cotizaciones.vigente(jugador, ahora)).thenReturn(
                new CotizacionVigentePort.CotizacionVigente(new BigDecimal("4"), null, null, null));

        PortfolioResultado resultado = service.portfolio(1L, 1L);

        assertEquals(new BigDecimal("-12.0000"), resultado.ganancia());
        assertEquals(new BigDecimal("-60.00"), resultado.gananciaPorcentaje());
        assertEquals(new BigDecimal("-12.0000"), resultado.posiciones().get(0).ganancia());
    }

    @Test
    void reconstruyeComprasYVentasEnUtcYConsultaPreciosUnaVezPorFecha() {
        Usuario contraparte = new Usuario("Super", "super-portfolio@example.com", "hash", ahora);
        Jugador segundoJugador = new Jugador("Mbappe", "Real Madrid", Liga.LA_LIGA, "Delantero");
        ReflectionTestUtils.setField(segundoJugador, "id", 4L);
        Instant compra = Instant.parse("2026-10-25T23:30:00Z");
        Instant venta = Instant.parse("2026-10-26T00:30:00Z");
        Transaccion compraTransaccion = new Transaccion(
                Transaccion.Tipo.COMPRA, usuario, jugador, 2, new BigDecimal("2.0000"),
                contraparte, compra, "activa", 1);
        Transaccion ventaTransaccion = new Transaccion(
                Transaccion.Tipo.VENTA, usuario, jugador, 1, new BigDecimal("3.0000"),
                contraparte, venta, "activa", 1);
        Transaccion segundaCompra = new Transaccion(
                Transaccion.Tipo.COMPRA, usuario, segundoJugador, 1, new BigDecimal("5.0000"),
                contraparte, venta, "activa", 1);
        when(transacciones.findFirstFechaByUsuarioId(1L)).thenReturn(Optional.of(compra));
        when(transacciones.findByUsuarioIdOrderByFechaAscIdAsc(1L))
                .thenReturn(List.of(compraTransaccion, ventaTransaccion, segundaCompra));
        var lunes = java.time.LocalDate.of(2026, 10, 26);
        when(cotizacionesHistoricas.fechasCotizacion(
                java.time.LocalDate.of(2026, 10, 25), lunes)).thenReturn(List.of(lunes));
        when(cotizacionesHistoricas.preciosA(List.of(3L, 4L), lunes))
                .thenReturn(java.util.Map.of(3L, new BigDecimal("4.0000"), 4L, new BigDecimal("6.0000")));

        EvolucionPortfolioResultado resultado = service.evolucion(
                1L, 1L, java.time.LocalDate.of(2026, 10, 25), lunes);

        assertEquals(new BigDecimal("7.0000"), resultado.puntos().get(0).invertido());
        assertEquals(new BigDecimal("10.0000"), resultado.puntos().get(0).valor());
        verify(cotizacionesHistoricas, times(1)).preciosA(List.of(3L, 4L), lunes);
    }

    @Test
    void usaDefaultsYDevuelveSerieVacia() {
        when(transacciones.findFirstFechaByUsuarioId(1L))
                .thenReturn(Optional.of(Instant.parse("2026-10-20T23:00:00Z")));
        when(cotizacionesHistoricas.fechasCotizacion(
                java.time.LocalDate.of(2026, 10, 20),
                java.time.LocalDate.of(2026, 10, 25))).thenReturn(List.of());

        EvolucionPortfolioResultado resultado = service.evolucion(1L, 1L, null, null);

        assertTrue(resultado.puntos().isEmpty());
    }

    @Test
    void rechazaIntervaloInvertidoYAccesoAjenoAntesDeConsultar() {
        java.time.LocalDate desde = java.time.LocalDate.of(2026, 10, 26);
        java.time.LocalDate hasta = java.time.LocalDate.of(2026, 10, 25);
        assertThrows(IllegalArgumentException.class, () -> service.evolucion(1L, 1L, desde, hasta));
        assertThrows(com.formula11.exception.AccesoPortfolioDenegadoException.class,
                () -> service.evolucion(9L, 1L, null, null));
        verifyNoInteractions(transacciones, cotizacionesHistoricas);
    }

    @Test
    void filtraEnBaseOrdenaYCompletaSaldoDesdeUnaConsultaDeAuditoria() {
        Usuario contraparte = new Usuario("Super", "super-history@example.com", "hash", ahora);
        Transaccion transaccion = new Transaccion(
                Transaccion.Tipo.COMPRA, usuario, jugador, 2, new BigDecimal("2.0000"),
                contraparte, ahora, "activa", 1);
        ReflectionTestUtils.setField(transaccion, "id", 7L);
        var auditoria = new AuditoriaMercado(
                usuario, transaccion, "{\"saldoUsuario\":100.0000}",
                "{\"saldoUsuario\":96.0000}", ahora);
        when(transacciones.buscarHistorial(eq(1L), eq(Transaccion.Tipo.COMPRA), eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaccion), Pageable.ofSize(20), 1));
        when(auditorias.findByOperacionIdIn(List.of(7L))).thenReturn(List.of(auditoria));

        HistorialTransaccionesResultado resultado = service.historial(
                1L, 1L, Transaccion.Tipo.COMPRA, 3L, 0, 20);

        assertEquals(0, new BigDecimal("96.0000")
                .compareTo(resultado.contenido().get(0).saldoPosterior()));
        verify(auditorias, times(1)).findByOperacionIdIn(List.of(7L));
        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(transacciones).buscarHistorial(eq(1L), eq(Transaccion.Tipo.COMPRA), eq(3L),
                pageableCaptor.capture());
        assertEquals("fecha: DESC,id: DESC", pageableCaptor.getValue().getSort().toString());
    }

    @Test
    void devuelvePaginaVaciaSinConsultarAuditorias() {
        when(transacciones.buscarHistorial(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20), 0));

        HistorialTransaccionesResultado resultado = service.historial(1L, 1L, null, null, 0, 20);

        assertTrue(resultado.contenido().isEmpty());
        verifyNoInteractions(auditorias);
    }

    @Test
    void auditoriaFaltanteEsErrorExplicito() {
        Usuario contraparte = new Usuario("Super", "super-history-missing@example.com", "hash", ahora);
        Transaccion transaccion = new Transaccion(
                Transaccion.Tipo.COMPRA, usuario, jugador, 1, BigDecimal.ONE,
                contraparte, ahora, null, null);
        ReflectionTestUtils.setField(transaccion, "id", 8L);
        when(transacciones.buscarHistorial(anyLong(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaccion)));
        when(auditorias.findByOperacionIdIn(List.of(8L))).thenReturn(List.of());

        assertThrows(IllegalStateException.class,
                () -> service.historial(1L, 1L, null, null, 0, 20));
    }

    @Test
    void rechazaPortfolioDeOtroUsuarioSinBuscarlo() {
        assertThrows(com.formula11.exception.AccesoPortfolioDenegadoException.class,
                () -> service.portfolio(1L, 2L));
        verify(usuarios, never()).findById(2L);
        verifyNoInteractions(cuentas, tenencias, cotizaciones);
    }

    @Test
    void rechazaPortfolioDelSuperusuario() {
        Usuario superusuario = new Usuario("Super", "super-portfolio@example.com", "hash", ahora);
        superusuario.asignarRol(Rol.SUPERUSUARIO);
        ReflectionTestUtils.setField(superusuario, "id", 9L);
        when(usuarios.findById(9L)).thenReturn(Optional.of(superusuario));

        assertThrows(com.formula11.exception.AccesoPortfolioDenegadoException.class,
                () -> service.portfolio(9L, 9L));
        verifyNoInteractions(cuentas, tenencias, cotizaciones);
    }
}
