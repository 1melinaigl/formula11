package com.formula11.adapter;

public class FootballDataException extends RuntimeException {
    public enum Tipo { NO_CONFIGURADO, NO_AUTORIZADO, LIMITE, PROVEEDOR, TIMEOUT, RESPUESTA_INVALIDA }

    private final Tipo tipo;

    public FootballDataException(Tipo tipo, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
    }

    public FootballDataException(Tipo tipo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }
}
