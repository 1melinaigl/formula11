package com.formula11.controller;

import com.formula11.dto.OperacionResponse;
import com.formula11.dto.OrdenRequest;
import com.formula11.service.MercadoService;
import com.formula11.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/ordenes")
public class MercadoController {
    private final MercadoService mercadoService;
    private final UsuarioService usuarioService;

    public MercadoController(MercadoService mercadoService, UsuarioService usuarioService) {
        this.mercadoService = mercadoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/compra")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Compra tokens de un jugador", description = "Compra tokens al superusuario usando la cuenta del usuario autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra realizada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
            @ApiResponse(responseCode = "403", description = "El superusuario no puede operar en el mercado"),
            @ApiResponse(responseCode = "404", description = "Jugador o usuario inexistente"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente"),
            @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    public OperacionResponse comprar(@Valid @RequestBody OrdenRequest request, Authentication authentication) {
        Long usuarioId = usuarioService.obtenerIdPorEmail(authentication.getName());
        return OperacionResponse.from(mercadoService.comprar(usuarioId, request.jugadorId(), request.cantidad()));
    }

    @PostMapping("/venta")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vende tokens de un jugador", description = "Vende tokens al superusuario usando la cuenta del usuario autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venta realizada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "401", description = "Autenticación requerida"),
            @ApiResponse(responseCode = "403", description = "El superusuario no puede operar en el mercado"),
            @ApiResponse(responseCode = "404", description = "Jugador o usuario inexistente"),
            @ApiResponse(responseCode = "422", description = "Tenencia insuficiente")
    })
    public OperacionResponse vender(@Valid @RequestBody OrdenRequest request, Authentication authentication) {
        Long usuarioId = usuarioService.obtenerIdPorEmail(authentication.getName());
        return OperacionResponse.from(mercadoService.vender(usuarioId, request.jugadorId(), request.cantidad()));
    }
}
