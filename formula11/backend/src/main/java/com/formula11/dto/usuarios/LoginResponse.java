package com.formula11.dto.usuarios;

import com.formula11.model.usuarios.Rol;
import com.formula11.service.usuarios.UsuarioAutenticado;

public record LoginResponse(Long id, String nombre, String email, Rol rol, String token) {
    public static LoginResponse from(UsuarioAutenticado usuario) {
        return new LoginResponse(usuario.id(), usuario.nombre(), usuario.email(), usuario.rol(), usuario.token());
    }
}
