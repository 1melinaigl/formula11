package com.formula11.dto.usuarios;

import com.formula11.service.usuarios.UsuarioAutenticado;
import java.time.Instant;

public record RegistroUsuarioResponse(Long id, String nombre, String email, Instant fechaRegistro, String token) {
    public static RegistroUsuarioResponse from(UsuarioAutenticado usuario) {
        return new RegistroUsuarioResponse(usuario.id(), usuario.nombre(), usuario.email(), usuario.fechaRegistro(), usuario.token());
    }
}
