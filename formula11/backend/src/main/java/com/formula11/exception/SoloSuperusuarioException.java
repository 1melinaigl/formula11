package com.formula11.exception;

/** La consulta pide datos que solo puede ver el superusuario, como una estrategia que no es la activa. */
public class SoloSuperusuarioException extends RuntimeException {
    public SoloSuperusuarioException(String mensaje) {
        super(mensaje);
    }
}
