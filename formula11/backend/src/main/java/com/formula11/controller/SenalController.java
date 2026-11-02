package com.formula11.controller;

import com.formula11.dto.AciertosResponse;
import com.formula11.dto.SenalesPageResponse;
import com.formula11.service.SenalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de la feature 010: señales de jugadores subvaluados y panel de aciertos. */
@RestController
@RequestMapping("/api/senales")
public class SenalController {
    private final SenalService service;

    public SenalController(SenalService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Señales de jugadores subvaluados",
            description = "Con vigentes=true (por defecto), solo las del último recálculo. "
                    + "El frontend consulta cada 30 segundos y muestra como alerta las señales cuyo id todavía no vio.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Página de señales, posiblemente vacía")
    @ApiResponse(responseCode = "400", description = "Paginación inválida")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    public SenalesPageResponse listar(
            @Parameter(description = "Solo las señales del último recálculo") @RequestParam(defaultValue = "true") boolean vigentes,
            @Parameter(description = "Índice de página base 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página, entre 1 y 100") @RequestParam(defaultValue = "20") int size) {
        return SenalesPageResponse.from(service.listar(vigentes, page, size));
    }

    @GetMapping("/aciertos")
    @Operation(summary = "Panel de aciertos de las señales",
            description = "Por cada semana con señales, compara cuánto subieron los jugadores señalados "
                    + "contra el promedio del mercado una semana después.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Resultados medidos por semana")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    public AciertosResponse aciertos() {
        return AciertosResponse.from(service.aciertos());
    }
}
