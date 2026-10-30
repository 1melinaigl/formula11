package com.formula11.service;

import java.time.LocalDate;

/** Genera las señales de subvaluados de una semana recalculada (feature 010). */
public interface GeneradorSenalesPort {
    void generar(LocalDate domingo);
}
