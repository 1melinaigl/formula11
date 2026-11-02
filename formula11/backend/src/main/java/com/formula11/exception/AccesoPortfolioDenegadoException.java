package com.formula11.exception;

public class AccesoPortfolioDenegadoException extends RuntimeException {
    public AccesoPortfolioDenegadoException() {
        super("No tiene permiso para consultar este portfolio");
    }
}
