package com.formula11.controller;

import com.formula11.dto.MercadoJugadorResponse;
import com.formula11.service.MercadoService;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/jugadores")
public class MercadoJugadorController {
    private final MercadoService mercadoService;

    public MercadoJugadorController(MercadoService mercadoService) {
        this.mercadoService = mercadoService;
    }

    @GetMapping("/{jugadorId}/mercado")
    @Operation(summary = "Consulta el mercado de un jugador")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Datos de mercado obtenidos")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    @ApiResponse(responseCode = "404", description = "Jugador inexistente")
    public MercadoJugadorResponse consultarMercado(@PathVariable Long jugadorId) {
        return MercadoJugadorResponse.from(mercadoService.consultarMercado(jugadorId));
    }
}
