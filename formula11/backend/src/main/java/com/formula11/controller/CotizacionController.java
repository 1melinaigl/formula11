package com.formula11.controller;

import com.formula11.dto.*;
import com.formula11.service.RecalculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {
    private final RecalculoService service;
    public CotizacionController(RecalculoService service) { this.service = service; }
    @Operation(summary = "Recalcula las cotizaciones")
    @ApiResponse(responseCode = "200", description = "Recálculo terminado")
    @PostMapping("/recalcular")
    @PreAuthorize("hasRole('SUPERUSUARIO')")
    public RecalculoResponse recalcular(@Valid @RequestBody(required = false) RecalculoRequest request) {
        return service.recalcular(request);
    }
}
