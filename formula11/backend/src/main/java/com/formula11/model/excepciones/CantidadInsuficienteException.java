package com.formula11.model.excepciones;

/**
 * Una operación pide más de lo disponible: créditos de una cuenta o tokens de una tenencia.
 * Excepción propia del modelo; el handler web la responde con 409.
 */
public class CantidadInsuficienteException extends RuntimeException {
    public CantidadInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
