package com.formula11.dto;

import com.formula11.model.senales.MedidorAciertos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Esquema {@code AciertosResponse}. */
public record AciertosResponse(List<Semana> semanas, BigDecimal tasaAcierto, BigDecimal diferenciaPromedio) {
    public record Semana(LocalDate fecha, int senales, BigDecimal variacionSenalados, BigDecimal variacionMercado,
                         int aciertos) {
    }

    public static AciertosResponse from(MedidorAciertos.Resultado resultado) {
        return new AciertosResponse(resultado.semanas().stream()
                .map(semana -> new Semana(semana.fecha(), semana.senales(), semana.variacionSenalados(),
                        semana.variacionMercado(), semana.aciertos()))
                .toList(), resultado.tasaAcierto(), resultado.diferenciaPromedio());
    }
}
