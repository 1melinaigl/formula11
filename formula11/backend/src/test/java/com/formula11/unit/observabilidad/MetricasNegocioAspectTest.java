package com.formula11.unit.observabilidad;

import com.formula11.model.jugadores.Jugador;
import com.formula11.model.jugadores.Liga;
import com.formula11.model.mercado.Transaccion;
import com.formula11.model.usuarios.Usuario;
import com.formula11.observabilidad.MetricasNegocio;
import com.formula11.observabilidad.MetricasNegocioAspect;
import com.formula11.observabilidad.TipoOperacionMercado;
import com.formula11.service.mercado.ResultadoOperacion;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MetricasNegocioAspectTest {
    private final MetricasNegocio metricas = mock(MetricasNegocio.class);
    private final MetricasNegocioAspect aspecto = new MetricasNegocioAspect(metricas);
    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);

    @Test
    void unaCompraConfirmadaSeCuentaComoCompra() {
        aspecto.contarOperacion(resultado(Transaccion.Tipo.COMPRA));

        verify(metricas).registrarOperacionMercado(TipoOperacionMercado.COMPRA);
    }

    @Test
    void unaVentaConfirmadaSeCuentaComoVenta() {
        aspecto.contarOperacion(resultado(Transaccion.Tipo.VENTA));

        verify(metricas).registrarOperacionMercado(TipoOperacionMercado.VENTA);
    }

    @Test
    void sinResultadoNoCuentaNiRompe() {
        assertDoesNotThrow(() -> aspecto.contarOperacion(null));

        verify(metricas, never()).registrarOperacionMercado(any());
    }

    @Test
    void siLaMetricaFallaLaOperacionSigueIgual() {
        doThrow(new IllegalStateException("registry roto")).when(metricas).registrarOperacionMercado(any());

        assertDoesNotThrow(() -> aspecto.contarOperacion(resultado(Transaccion.Tipo.COMPRA)));
    }

    @Test
    void unRecalculoQueTerminaBienSeMideComoOk() throws Throwable {
        Timer.Sample muestra = mock(Timer.Sample.class);
        when(metricas.iniciarMedicion()).thenReturn(muestra);
        when(joinPoint.proceed()).thenReturn("resultado");

        Object resultado = aspecto.medirRecalculo(joinPoint);

        assertEquals("resultado", resultado);
        verify(metricas).registrarRecalculo(muestra, true);
    }

    @Test
    void unRecalculoQueFallaSeMideComoErrorYRelanzaLaMismaExcepcion() throws Throwable {
        Timer.Sample muestra = mock(Timer.Sample.class);
        when(metricas.iniciarMedicion()).thenReturn(muestra);
        IllegalStateException error = new IllegalStateException("Ya hay un recálculo en curso");
        when(joinPoint.proceed()).thenThrow(error);

        IllegalStateException lanzada = assertThrows(IllegalStateException.class, () -> aspecto.medirRecalculo(joinPoint));

        assertSame(error, lanzada);
        verify(metricas).registrarRecalculo(muestra, false);
    }

    @Test
    void siMedirFallaElRecalculoDevuelveSuResultado() throws Throwable {
        when(metricas.iniciarMedicion()).thenThrow(new IllegalStateException("registry roto"));
        doThrow(new IllegalStateException("registry roto")).when(metricas).registrarRecalculo(any(), anyBoolean());
        when(joinPoint.proceed()).thenReturn("resultado");

        assertEquals("resultado", aspecto.medirRecalculo(joinPoint));
    }

    private static ResultadoOperacion resultado(Transaccion.Tipo tipo) {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash");
        Jugador jugador = new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "DEL");
        Transaccion transaccion = new Transaccion(tipo, usuario, jugador, 1, BigDecimal.ONE,
                new Usuario("Super", "super@example.com", "hash"), Instant.parse("2026-11-26T12:00:00Z"), null, null);
        return new ResultadoOperacion(transaccion, BigDecimal.TEN);
    }
}
