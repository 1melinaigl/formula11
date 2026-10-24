package com.formula11.unit.service;

import com.formula11.exception.*;
import com.formula11.model.*;
import com.formula11.persistence.*;
import com.formula11.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
// El @BeforeEach prepara el escenario completo de compra; los casos de rechazo cortan antes y no usan todos los stubs.
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MercadoServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock JugadorRepository jugadores;
    @Mock CuentaRepository cuentas;
    @Mock TenenciaRepository tenencias;
    @Mock TransaccionRepository transacciones;
    @Mock AuditoriaMercadoRepository auditorias;
    @Mock CotizacionVigentePort cotizaciones;
    MercadoService service;
    Usuario comprador;
    Usuario superusuario;
    Jugador jugador;
    Cuenta cuenta;
    Tenencia stock;
    Instant ahora = Instant.parse("2026-10-24T17:00:00Z");

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(ahora, ZoneOffset.UTC);
        service = new MercadoService(usuarios, jugadores, cuentas, tenencias, transacciones, auditorias, cotizaciones, clock);
        comprador = new Usuario("Ana", "ana-service@example.com", "hash");
        superusuario = new Usuario("Super", "super-service@example.com", "hash");
        superusuario.asignarRol(Rol.SUPERUSUARIO);
        jugador = new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero");
        ReflectionTestUtils.setField(comprador, "id", 1L);
        ReflectionTestUtils.setField(superusuario, "id", 2L);
        ReflectionTestUtils.setField(jugador, "id", 3L);
        cuenta = new Cuenta(comprador, new BigDecimal("100"));
        stock = new Tenencia(superusuario, jugador, 100, BigDecimal.ONE);
        when(usuarios.findById(1L)).thenReturn(Optional.of(comprador));
        when(usuarios.findByRol(Rol.SUPERUSUARIO)).thenReturn(Optional.of(superusuario));
        when(jugadores.findById(3L)).thenReturn(Optional.of(jugador));
        when(cuentas.findByUsuarioIdForUpdate(1L)).thenReturn(Optional.of(cuenta));
        when(tenencias.findByUsuarioIdAndJugadorIdForUpdate(2L, 3L)).thenReturn(Optional.of(stock));
        when(tenencias.findByUsuarioIdAndJugadorIdForUpdate(1L, 3L)).thenReturn(Optional.empty());
        when(cotizaciones.vigente(jugador, ahora)).thenReturn(new CotizacionVigentePort.Cotizacion(BigDecimal.ONE, null, null, null));
        when(transacciones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void compraDescuentaSaldoTransfiereTokensYAudita() {
        ResultadoOperacion resultado = service.comprar(1L, 3L, 2);
        assertEquals(new BigDecimal("98.0000"), resultado.saldoPosterior());
        assertEquals(2, resultado.transaccion().getCantidad());
        assertEquals(98, stock.getCantidad());
        verify(transacciones).save(any(Transaccion.class));
        verify(auditorias).save(any());
    }

    @Test
    void rechazaSaldoInsuficienteSinPersistir() {
        cuenta.debitar(new BigDecimal("100"));
        assertThrows(OperacionNoValidaException.class, () -> service.comprar(1L, 3L, 1));
        verify(transacciones, never()).save(any());
        verify(auditorias, never()).save(any());
    }

    @Test
    void rechazaStockInsuficienteSinPersistir() {
        stock.vender(100);
        assertThrows(ConflictoMercadoException.class, () -> service.comprar(1L, 3L, 1));
        verify(transacciones, never()).save(any());
    }

    @Test
    void rechazaCompraDelSuperusuario() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(superusuario));
        assertThrows(SuperusuarioNoOperaException.class, () -> service.comprar(2L, 3L, 1));
        verifyNoInteractions(jugadores, cotizaciones, cuentas);
    }

    @Test
    void rechazaCotizacionInvalida() {
        when(cotizaciones.vigente(jugador, ahora)).thenThrow(new IllegalArgumentException("cotización inválida"));
        assertThrows(IllegalArgumentException.class, () -> service.comprar(1L, 3L, 1));
        verify(transacciones, never()).save(any());
    }

    @Test
    void ventaAcreditaSaldoDevuelveTokensYConservaPromedio() {
        Tenencia tenencia = new Tenencia(comprador, jugador, 2, new BigDecimal("3.5000"));
        when(tenencias.findByUsuarioIdAndJugadorIdForUpdate(1L, 3L)).thenReturn(Optional.of(tenencia));

        ResultadoOperacion resultado = service.vender(1L, 3L, 2);

        assertEquals(new BigDecimal("102.0000"), resultado.saldoPosterior());
        assertEquals(Transaccion.Tipo.VENTA, resultado.transaccion().getTipo());
        assertEquals(0, tenencia.getCantidad());
        assertEquals(new BigDecimal("3.5000"), tenencia.getPrecioPromedio());
        assertEquals(102, stock.getCantidad());
        verify(transacciones).save(any(Transaccion.class));
        verify(auditorias).save(any());
    }

    @Test
    void ventaRechazaTenenciaInsuficienteSinPersistir() {
        Tenencia tenencia = new Tenencia(comprador, jugador, 1, BigDecimal.ONE);
        when(tenencias.findByUsuarioIdAndJugadorIdForUpdate(1L, 3L)).thenReturn(Optional.of(tenencia));

        assertThrows(OperacionNoValidaException.class, () -> service.vender(1L, 3L, 2));

        assertEquals(new BigDecimal("100.0000"), cuenta.getSaldo());
        assertEquals(1, tenencia.getCantidad());
        assertEquals(100, stock.getCantidad());
        verify(transacciones, never()).save(any());
        verify(auditorias, never()).save(any());
    }

    @Test
    void ventaRechazaAlSuperusuario() {
        when(usuarios.findById(2L)).thenReturn(Optional.of(superusuario));

        assertThrows(SuperusuarioNoOperaException.class, () -> service.vender(2L, 3L, 1));

        verifyNoInteractions(jugadores, cotizaciones, cuentas, tenencias);
    }

    @Test
    void ventaNoPersisteSiLaAuditoriaFalla() {
        Tenencia tenencia = new Tenencia(comprador, jugador, 2, BigDecimal.ONE);
        when(tenencias.findByUsuarioIdAndJugadorIdForUpdate(1L, 3L)).thenReturn(Optional.of(tenencia));
        doThrow(new IllegalStateException("fallo de auditoría")).when(auditorias).save(any());

        assertThrows(IllegalStateException.class, () -> service.vender(1L, 3L, 1));

        verify(transacciones).save(any(Transaccion.class));
        verify(auditorias).save(any());
    }

    @Test
    void consultaMercadoDevuelveStockCotizacionYFecha() {
        when(tenencias.findByUsuarioIdAndJugadorId(2L, 3L)).thenReturn(Optional.of(stock));

        MercadoJugadorResultado resultado = service.consultarMercado(3L);

        assertEquals(3L, resultado.jugadorId());
        assertEquals(100, resultado.tokensEmitidos());
        assertEquals(100, resultado.tokensDisponibles());
        assertEquals(new BigDecimal("1.0000"), resultado.precioVigente());
        assertNull(resultado.fechaCotizacion());
    }
}
