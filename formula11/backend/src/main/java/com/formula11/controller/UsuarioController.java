package com.formula11.controller;

import com.formula11.dto.RegistroUsuarioRequest;
import com.formula11.dto.RegistroUsuarioResponse;
import com.formula11.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

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
}
