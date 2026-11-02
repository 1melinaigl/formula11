package com.formula11.unit.exception;

import com.formula11.exception.GlobalExceptionHandler;
import com.formula11.model.excepciones.CantidadInsuficienteException;
import com.formula11.model.excepciones.DatoInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/** Las excepciones propias del modelo se responden con los mismos códigos que antes tenían las genéricas. */
class ExcepcionesDominioHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unDatoInvalidoDelDominioResponde400ConSuMensaje() {
        var respuesta = handler.datoInvalido(new DatoInvalidoException("El email no es válido"), new MockHttpServletRequest());

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals("El email no es válido", respuesta.getBody().mensaje());
    }

    @Test
    void unaCantidadInsuficienteResponde409() {
        var respuesta = handler.cantidadInsuficiente(new CantidadInsuficienteException("El saldo es insuficiente"),
                new MockHttpServletRequest());

        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals("El saldo es insuficiente", respuesta.getBody().mensaje());
    }

    @Test
    void requeridoDevuelveElValorOLanzaLaExcepcionDelDominio() {
        assertEquals("valor", DatoInvalidoException.requerido("valor", "obligatorio"));

        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> DatoInvalidoException.requerido(null, "La liga es obligatoria"));
        assertEquals("La liga es obligatoria", error.getMessage());
    }
}
