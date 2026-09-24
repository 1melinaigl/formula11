package com.formula11.controller;

import com.formula11.dto.RegistroUsuarioRequest;
import com.formula11.dto.RegistroUsuarioResponse;
import com.formula11.dto.LoginRequest;
import com.formula11.dto.LoginResponse;
import com.formula11.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registro de usuario", description = "Registra un nuevo usuario en el sistema")
    @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente")
    public RegistroUsuarioResponse registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
        return usuarioService.registrar(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login de usuario", description = "Autentica un usuario registrado y emite un JWT")
    @ApiResponse(responseCode = "200", description = "Login exitoso")
    @ApiResponse(responseCode = "400", description = "Datos de login inválidos")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return usuarioService.login(request);
    }
}
