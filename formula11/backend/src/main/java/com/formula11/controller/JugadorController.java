package com.formula11.controller;

import com.formula11.dto.JugadorResponse;
import com.formula11.service.JugadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/jugadores")
public class JugadorController {
    private final JugadorService jugadorService;

    public JugadorController(JugadorService jugadorService) {
        this.jugadorService = jugadorService;
    }

    @Operation(summary = "Listado de jugadores", description = "Devuelve el catálogo de jugadores registrados")
    @ApiResponse(responseCode = "200", description = "Catálogo obtenido correctamente")
    @GetMapping
    public List<JugadorResponse> listar() {
        return jugadorService.listar();
    }
}
