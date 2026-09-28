package com.formula11.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Las credenciales no son válidas");
    }
}
