package com.formula11.controller;

import com.formula11.dto.*;
import com.formula11.model.EjecucionMetricas;
import com.formula11.persistence.EjecucionMetricaLigaRepository;
import com.formula11.persistence.PendienteVinculacionRepository;
import com.formula11.service.CargaMetricasService;
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

@RestController
@RequestMapping("/api/metricas")
@PreAuthorize("hasRole('SUPERUSUARIO')")
@Validated
public class MetricasController {
    private final CargaMetricasService service;
    private final EjecucionMetricaLigaRepository ligaRepository;
    private final PendienteVinculacionRepository pendienteRepository;

    public MetricasController(CargaMetricasService service,
                               EjecucionMetricaLigaRepository ligaRepository,
                               PendienteVinculacionRepository pendienteRepository) {
        this.service = service;
        this.ligaRepository = ligaRepository;
        this.pendienteRepository = pendienteRepository;
    }

    @PostMapping("/sincronizacion")
    @Operation(summary = "Dispara la carga de métricas desde WhoScored")
    @ApiResponse(responseCode = "202", description = "Carga iniciada")
    public ResponseEntity<EjecucionMetricasResponse> sincronizar(
            @Valid @RequestBody(required = false) SincronizacionMetricasRequest request) {
        EjecucionMetricas ejecucion = service.iniciar(request == null ? null : request.desde());
        return ResponseEntity.accepted().body(service.response(ejecucion, java.util.List.of()));
    }

    @GetMapping("/sincronizacion/{ejecucionId}")
    @Operation(summary = "Consulta el estado de una carga de métricas")
    public EjecucionMetricasResponse estado(@PathVariable Long ejecucionId) {
        EjecucionMetricas ejecucion = service.obtener(ejecucionId);
        return service.response(ejecucion, ligaRepository.findByEjecucionId(ejecucionId));
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Lista jugadores pendientes de vinculación")
    public PendientesPageResponse pendientes(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.pendientes(PageRequest.of(page, size), pendienteRepository);
    }
}
