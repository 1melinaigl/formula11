package com.formula11.controller.metricas;

import com.formula11.model.EjecucionMetricas;
import com.formula11.service.metricas.CargaMetricasService;
import com.formula11.dto.metricas.EjecucionMetricasResponse;
import com.formula11.dto.metricas.PendientesPageResponse;
import com.formula11.dto.metricas.SincronizacionMetricasRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/metricas")
@PreAuthorize("hasRole('SUPERUSUARIO')")
@Validated
public class MetricasController {
    private final CargaMetricasService service;

    public MetricasController(CargaMetricasService service) {
        this.service = service;
    }

    @PostMapping("/sincronizacion")
    @Operation(summary = "Dispara la carga de métricas desde WhoScored")
    @ApiResponse(responseCode = "202", description = "Carga iniciada")
    public ResponseEntity<EjecucionMetricasResponse> sincronizar(
            @Valid @RequestBody(required = false) SincronizacionMetricasRequest request) {
        EjecucionMetricas ejecucion = service.iniciar(request == null ? null : request.desde());
        return ResponseEntity.accepted().body(EjecucionMetricasResponse.from(service.resumen(ejecucion, List.of())));
    }

    @GetMapping("/sincronizacion/{ejecucionId}")
    @Operation(summary = "Consulta el estado de una carga de métricas")
    public EjecucionMetricasResponse estado(@PathVariable Long ejecucionId) {
        return EjecucionMetricasResponse.from(service.estado(ejecucionId));
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Lista jugadores pendientes de vinculación")
    public PendientesPageResponse pendientes(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PendientesPageResponse.from(service.pendientes(PageRequest.of(page, size)));
    }
}
