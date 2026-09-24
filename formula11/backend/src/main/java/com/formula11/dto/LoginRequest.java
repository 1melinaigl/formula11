package com.formula11.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Size(max = 254, message = "El email no puede superar los 254 caracteres")
        @Pattern(regexp = "^\\s*[^\\s@]+@[^\\s@.]+\\.[^\\s@]+\\s*$", message = "El email no es válido")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password
) { }
