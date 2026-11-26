package com.formula11.unit.observabilidad;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.formula11.observabilidad.EnmascaradorParametros;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EnmascaradorParametrosTest {
    // Igual que el ObjectMapper de Spring Boot: fechas como texto ISO y no como arreglo.
    private final EnmascaradorParametros enmascarador = new EnmascaradorParametros(JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build());

    record Orden(Long jugadorId, int cantidad) { }

    static class NoSerializable {
        public String getValor() {
            throw new IllegalStateException("no se puede leer");
        }

        @Override
        public String toString() {
            return "NoSerializable";
        }
    }

    @Test
    void sinArgumentosDevuelveLlavesVacias() {
        assertEquals("{}", enmascarador.enmascarar(new String[0], new Object[0]));
    }

    @Test
    void muestraValoresSimplesYRecordsComoJson() {
        String parametros = enmascarador.enmascarar(
                new String[]{"usuarioId", "liga", "request", "desde"},
                new Object[]{3L, "La Liga", new Orden(12L, 5), LocalDate.of(2026, 11, 26)});

        assertEquals(
                "{usuarioId=3, liga=La Liga, request={\"jugadorId\":12,\"cantidad\":5}, desde=2026-11-26}",
                parametros);
    }

    @Test
    void muestraNullComoNull() {
        assertEquals("{liga=null}", enmascarador.enmascarar(new String[]{"liga"}, new Object[]{null}));
    }

    @Test
    void omiteLosArgumentosDeInfraestructura() {
        Object autenticacion = new UsernamePasswordAuthenticationToken("ana@mail.com", null, List.of());
        Object request = mock(HttpServletRequest.class);
        Object errores = new BeanPropertyBindingResult(new Orden(1L, 1), "orden");

        String parametros = enmascarador.enmascarar(
                new String[]{"usuarioId", "authentication", "httpRequest", "bindingResult"},
                new Object[]{3L, autenticacion, request, errores});

        assertEquals("{usuarioId=3}", parametros);
    }

    @Test
    void siNoSePuedeSerializarUsaToString() {
        assertEquals("{valor=NoSerializable}",
                enmascarador.enmascarar(new String[]{"valor"}, new Object[]{new NoSerializable()}));
    }

    @Test
    void recortaLosParametrosLargos() {
        String parametros = enmascarador.enmascarar(new String[]{"texto"}, new Object[]{"x".repeat(1000)});

        assertEquals(EnmascaradorParametros.LARGO_MAXIMO + EnmascaradorParametros.MARCA_RECORTE.length(),
                parametros.length());
        assertTrue(parametros.endsWith(EnmascaradorParametros.MARCA_RECORTE));
        assertTrue(parametros.startsWith("{texto=xxx"));
    }
}
