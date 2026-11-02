package com.formula11.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrdenRequest(
        @NotNull(message = "El jugador es obligatorio") Long jugadorId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 100, message = "La cantidad no puede superar 100") Integer cantidad
) { }
