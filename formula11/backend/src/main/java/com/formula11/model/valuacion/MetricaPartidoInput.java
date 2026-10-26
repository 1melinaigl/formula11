package com.formula11.model.valuacion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Persistence-neutral match statistics consumed by valuation strategies. */
public record MetricaPartidoInput(long jugadorId, String liga, String posicion, LocalDate fecha,
                                  Integer minutos, Boolean titular, Integer goles, Integer asistencias,
                                  Integer tiros, Integer pasesClave, Integer regates, Integer entradas,
                                  Integer intercepciones, Integer amarillas, Integer rojas, Integer atajadas,
                                  Integer golesRecibidos, Integer vallaInvicta, BigDecimal rating) {
    public MetricaPartidoInput {
        if (fecha == null || posicion == null || posicion.isBlank()) throw new IllegalArgumentException("Datos incompletos");
        if (minutos != null && minutos < 0) throw new IllegalArgumentException("Los minutos no pueden ser negativos");
    }
    public Integer goles() { return nz(goles); }
    public Integer asistencias() { return nz(asistencias); }
    public Integer tiros() { return nz(tiros); }
    public Integer pasesClave() { return nz(pasesClave); }
    public Integer regates() { return nz(regates); }
    public Integer entradas() { return nz(entradas); }
    public Integer intercepciones() { return nz(intercepciones); }
    public Integer amarillas() { return nz(amarillas); }
    public Integer rojas() { return nz(rojas); }
    public Integer atajadas() { return nz(atajadas); }
    public Integer golesRecibidos() { return nz(golesRecibidos); }
    public Integer vallaInvicta() { return nz(vallaInvicta); }
    public boolean jugo() { return minutos != null && minutos > 0; }
    private static int nz(Integer value) { return value == null ? 0 : value; }
}
