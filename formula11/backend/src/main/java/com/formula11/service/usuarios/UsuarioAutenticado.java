package com.formula11.service.usuarios;

import com.formula11.model.usuarios.Rol;
import com.formula11.model.usuarios.Usuario;

import java.time.Instant;

/** Usuario recién registrado o logueado, con el token JWT de su sesión. */
public record UsuarioAutenticado(Long id, String nombre, String email, Rol rol, Instant fechaRegistro, String token) {
    static UsuarioAutenticado de(Usuario usuario, String token) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol(),
                usuario.getFechaRegistro(), token);
    }
}
