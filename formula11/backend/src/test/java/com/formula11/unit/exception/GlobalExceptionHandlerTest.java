package com.formula11.unit.exception;

import com.formula11.exception.GlobalExceptionHandler;
import com.formula11.exception.AccesoPortfolioDenegadoException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    @Test
    void usaElCorrelationIdDelMdcCuandoNoHayHeaderEnLaRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", "generado-por-filtro")) {
            var respuesta = handler.argumento(new IllegalArgumentException("Parametro invalido"), new MockHttpServletRequest());

            assertEquals("generado-por-filtro", respuesta.getBody().correlationId());
        }
    }

    @Test
    void accesoAlPortfolioRespondeForbidden() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var respuesta = handler.accesoPortfolio(
                new AccesoPortfolioDenegadoException(), new MockHttpServletRequest());

        assertEquals(403, respuesta.getStatusCode().value());
    }
}
