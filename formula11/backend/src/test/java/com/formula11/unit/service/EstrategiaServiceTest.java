package com.formula11.unit.service;

import com.formula11.dto.NuevaVersionRequest;
import com.formula11.model.Estrategia;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.service.CotizacionCacheInvalidator;
import com.formula11.service.EstrategiaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class EstrategiaServiceTest {
    @Mock EstrategiaRepository repository;
    @Mock CotizacionCacheInvalidator invalidator;
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void listaEstrategiasYVersionesEnOrdenDescendente() {
        Estrategia rendimiento = estrategia("RENDIMIENTO_POSICION", 1, true);
        Estrategia consistencia = estrategia("CONSISTENCIA", 1, false);
        when(repository.findAll()).thenReturn(List.of(consistencia, rendimiento));
        when(repository.findByCodigoOrderByVersionDesc("RENDIMIENTO_POSICION"))
                .thenReturn(List.of(estrategia("RENDIMIENTO_POSICION", 2, false), rendimiento));

        var service = new EstrategiaService(repository, clock, invalidator);

        assertEquals(List.of("CONSISTENCIA", "RENDIMIENTO_POSICION"),
                service.listar().stream().map(r -> r.codigo()).toList());
        assertEquals(List.of(2, 1), service.versiones("RENDIMIENTO_POSICION")
                .stream().map(r -> r.version()).toList());
    }

    @Test
    void creaVersionInmutableConVersionIncrementada() {
        Estrategia actual = estrategia("RENDIMIENTO_POSICION", 1, true);
        when(repository.findTopByCodigoOrderByVersionDesc(actual.getCodigo())).thenReturn(Optional.of(actual));
        NuevaVersionRequest request = request();
        when(repository.save(any(Estrategia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new EstrategiaService(repository, clock, invalidator)
                .crearVersion(actual.getCodigo(), request);

        assertEquals(2, result.version());
        assertFalse(result.activa());
        assertTrue(result.creadaEn().equals(clock.instant()));
        verify(repository).save(argThat(e -> e.getVersion() == 2 && !e.isActiva()));
        assertTrue(actual.isActiva());
    }

    @Test
    void activaLaVersionMasNuevaEInvalidaLaCache() {
        Estrategia activa = estrategia("RENDIMIENTO_POSICION", 1, true);
        Estrategia nueva = estrategia("CONSISTENCIA", 2, false);
        when(repository.findTopByCodigoOrderByVersionDesc("CONSISTENCIA")).thenReturn(Optional.of(nueva));
        when(repository.findByActivaTrue()).thenReturn(Optional.of(activa));
        when(repository.save(any(Estrategia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = new EstrategiaService(repository, clock, invalidator).activar("CONSISTENCIA");

        assertTrue(result.activa());
        assertFalse(activa.isActiva());
        verify(invalidator).invalidate();
    }

    @Test
    void rechazaVersionSinPesosOParametros() {
        Estrategia actual = estrategia("RENDIMIENTO_POSICION", 1, true);
        when(repository.findTopByCodigoOrderByVersionDesc(actual.getCodigo())).thenReturn(Optional.of(actual));
        var service = new EstrategiaService(repository, clock, invalidator);

        assertThrows(IllegalArgumentException.class,
                () -> service.crearVersion(actual.getCodigo(), new NuevaVersionRequest(Map.of(), Map.of())));
    }

    @Test
    void rechazaParametroDePrecioNegativoYcodigoDesconocido() {
        Estrategia actual = estrategia("RENDIMIENTO_POSICION", 1, true);
        when(repository.findTopByCodigoOrderByVersionDesc(actual.getCodigo())).thenReturn(Optional.of(actual));
        var service = new EstrategiaService(repository, clock, invalidator);

        assertThrows(IllegalArgumentException.class, () -> service.crearVersion(actual.getCodigo(),
                new NuevaVersionRequest(Map.of("DEL", Map.of("goles", 1d)),
                        Map.of("k", -0.1d, "topeVariacion", 0.2d, "piso", 0.1d))));
        when(repository.findTopByCodigoOrderByVersionDesc("NO_EXISTE")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.activar("NO_EXISTE"));
    }

    @Test
    void rechazaMetricaYPosicionDesconocidas() {
        Estrategia actual = estrategia("RENDIMIENTO_POSICION", 1, true);
        when(repository.findTopByCodigoOrderByVersionDesc(actual.getCodigo())).thenReturn(Optional.of(actual));
        var service = new EstrategiaService(repository, clock, invalidator);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> service.crearVersion(actual.getCodigo(),
                        new NuevaVersionRequest(Map.of("ATAQUE", Map.of("goles", 1d)), request().parametros()))),
                () -> assertThrows(IllegalArgumentException.class, () -> service.crearVersion(actual.getCodigo(),
                        new NuevaVersionRequest(Map.of("DEL", Map.of("posesiones", 1d)), request().parametros()))));
    }

    private static NuevaVersionRequest request() {
        return new NuevaVersionRequest(Map.of("DEL", Map.of("goles", 1d)),
                Map.of("k", 0.4d, "topeVariacion", 0.2d, "piso", 0.1d));
    }

    private static Estrategia estrategia(String codigo, int version, boolean activa) {
        return new Estrategia(codigo, codigo, version, Map.of("DEL", Map.of("goles", 1d)),
                Map.of("k", 0.4d, "topeVariacion", 0.2d, "piso", 0.1d), activa,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
