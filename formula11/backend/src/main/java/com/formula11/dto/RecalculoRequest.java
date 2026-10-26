package com.formula11.dto;

import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record RecalculoRequest(LocalDate desde, LocalDate hasta) {
    @AssertTrue(message = "Desde y hasta deben enviarse juntas y desde no puede ser posterior a hasta")
    public boolean rangoValido() {
        return (desde == null && hasta == null) || (desde != null && hasta != null && !desde.isAfter(hasta));
    }
}
