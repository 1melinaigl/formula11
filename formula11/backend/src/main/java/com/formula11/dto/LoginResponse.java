package com.formula11.dto;

import com.formula11.model.Usuario;

public record LoginResponse(Long id, String nombre, String email, com.formula11.model.Rol rol, String token) {
    public static LoginResponse from(Usuario usuario, String token) {
       return new LoginResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol(), token);
    }
}
