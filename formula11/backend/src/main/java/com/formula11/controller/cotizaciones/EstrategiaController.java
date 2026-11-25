package com.formula11.controller.cotizaciones;

import com.formula11.service.cotizaciones.EstrategiaService;
import com.formula11.service.cotizaciones.NuevaVersion;
import com.formula11.dto.cotizaciones.EstrategiaResponse;
import com.formula11.dto.cotizaciones.NuevaVersionRequest;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/estrategias")
public class EstrategiaController {
    private final EstrategiaService service;
    public EstrategiaController(EstrategiaService service) { this.service = service; }
    @Operation(summary = "Lista las estrategias")
    @ApiResponse(responseCode = "200", description = "Estrategias obtenidas")
    @GetMapping public List<EstrategiaResponse> listar() {
        return service.listar().stream().map(EstrategiaResponse::from).toList();
    }
    @Operation(summary = "Lista versiones de una estrategia")
    @GetMapping("/{codigo}/versiones") public List<EstrategiaResponse> versiones(@PathVariable String codigo) {
        return service.versiones(codigo).stream().map(EstrategiaResponse::from).toList();
    }
    @Operation(summary = "Crea una versión de estrategia")
    @PostMapping("/{codigo}/versiones") @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201")
    @PreAuthorize("hasRole('SUPERUSUARIO')")
    @ResponseStatus(HttpStatus.CREATED)
    public EstrategiaResponse crear(@PathVariable String codigo, @Valid @RequestBody NuevaVersionRequest request) {
        return EstrategiaResponse.from(
                service.crearVersion(codigo, new NuevaVersion(request.pesos(), request.parametros())));
    }
    @Operation(summary = "Activa una estrategia")
    @PostMapping("/{codigo}/activar")
    @PreAuthorize("hasRole('SUPERUSUARIO')")
    public EstrategiaResponse activar(@PathVariable String codigo) {
        return EstrategiaResponse.from(service.activar(codigo));
    }
}
