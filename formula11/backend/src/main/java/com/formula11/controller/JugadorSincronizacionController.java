package com.formula11.controller;

import com.formula11.dto.SincronizacionResponse;
import com.formula11.service.SincronizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jugadores")
public class JugadorSincronizacionController {
    private final SincronizacionService sincronizacionService;

    public JugadorSincronizacionController(SincronizacionService sincronizacionService) {
        this.sincronizacionService = sincronizacionService;
    }

    @PostMapping("/sincronizacion")
    @Operation(summary = "Sincroniza jugadores desde Football-Data.org", description = "Consulta las cinco ligas y actualiza el catálogo local. Requiere JWT.")
    @ApiResponse(responseCode = "200", description = "Sincronización completa o parcial")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    @ApiResponse(responseCode = "502", description = "El proveedor externo rechazó o no pudo procesar la solicitud")
    @ApiResponse(responseCode = "503", description = "La integración no está configurada o el proveedor no está disponible")
    public SincronizacionResponse sincronizar() {
        return sincronizacionService.sincronizar();
    }
}
