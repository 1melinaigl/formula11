package com.formula11.model.excepciones;

/**
 * Un dato viola una regla del dominio: falta un valor obligatorio, está fuera de rango o tiene un formato inválido.
 * Es la excepción propia del modelo para sus invariantes (constitución, punto 3); el handler web la responde con 400.
 */
public class DatoInvalidoException extends RuntimeException {
    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }

    public DatoInvalidoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /** Devuelve el valor si no es nulo; si es nulo, lanza la excepción con el mensaje del dominio. */
    public static <T> T requerido(T valor, String mensaje) {
        if (valor == null) {
            throw new DatoInvalidoException(mensaje);
        }
        return valor;
    }
}
