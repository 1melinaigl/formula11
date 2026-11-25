package com.formula11.controller.usuarios;

import com.formula11.dto.usuarios.RegistroUsuarioRequest;
import com.formula11.dto.usuarios.RegistroUsuarioResponse;
import com.formula11.dto.usuarios.LoginRequest;
import com.formula11.dto.usuarios.LoginResponse;
import com.formula11.service.usuarios.UsuarioService;
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
        return RegistroUsuarioResponse.from(
                usuarioService.registrar(request.nombre(), request.email(), request.password()));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login de usuario", description = "Autentica un usuario registrado y emite un JWT")
    @ApiResponse(responseCode = "200", description = "Login exitoso; la respuesta incluye el rol autenticado")
    @ApiResponse(responseCode = "400", description = "Datos de login inválidos")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(usuarioService.login(request.email(), request.password()));
    }
}
