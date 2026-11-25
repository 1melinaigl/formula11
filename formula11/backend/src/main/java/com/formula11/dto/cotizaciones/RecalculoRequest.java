package com.formula11.dto.cotizaciones;

import com.formula11.service.cotizaciones.RangoRecalculo;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record RecalculoRequest(LocalDate desde, LocalDate hasta) {
    @AssertTrue(message = "Desde y hasta deben enviarse juntas y desde no puede ser posterior a hasta")
    public boolean rangoValido() {
        return new RangoRecalculo(desde, hasta).esValido();
    }
}
