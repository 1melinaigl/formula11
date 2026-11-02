package com.formula11.exception;

public class SuperusuarioNoOperaException extends RuntimeException {
    public SuperusuarioNoOperaException() { super("El superusuario no puede operar en el mercado"); }
}
