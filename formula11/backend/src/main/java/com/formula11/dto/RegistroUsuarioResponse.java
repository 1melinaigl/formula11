package com.formula11.dto;

import com.formula11.model.Usuario;
import java.time.Instant;

public record RegistroUsuarioResponse(Long id, String nombre, String email, Instant fechaRegistro, String token) {
    public static RegistroUsuarioResponse from(Usuario usuario, String token) {
        return new RegistroUsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getFechaRegistro(), token);
    }
}
