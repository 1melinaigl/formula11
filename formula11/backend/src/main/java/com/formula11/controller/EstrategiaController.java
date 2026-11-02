package com.formula11.controller;

import com.formula11.dto.*;
import com.formula11.service.EstrategiaService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/estrategias")
public class EstrategiaController {
    private final EstrategiaService service;
    public EstrategiaController(EstrategiaService service) { this.service = service; }
    @Operation(summary = "Lista las estrategias")
    @ApiResponse(responseCode = "200", description = "Estrategias obtenidas")
    @GetMapping public List<EstrategiaResponse> listar() { return service.listar(); }
    @Operation(summary = "Lista versiones de una estrategia")
    @GetMapping("/{codigo}/versiones") public List<EstrategiaResponse> versiones(@PathVariable String codigo) { return service.versiones(codigo); }
    @Operation(summary = "Crea una versión de estrategia")
    @PostMapping("/{codigo}/versiones") @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPERUSUARIO')")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public EstrategiaResponse crear(@PathVariable String codigo, @Valid @RequestBody NuevaVersionRequest request) { return service.crearVersion(codigo, request); }
    @Operation(summary = "Activa una estrategia")
    @PostMapping("/{codigo}/activar")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPERUSUARIO')")
    public EstrategiaResponse activar(@PathVariable String codigo) { return service.activar(codigo); }
}
