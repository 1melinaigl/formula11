package com.formula11.controller;

import com.formula11.dto.JugadoresPageResponse;
import com.formula11.service.JugadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/jugadores")
public class JugadorController {
    private static final Set<String> PARAMETROS_VALIDOS = Set.of("page", "size", "sort", "liga", "equipo", "posicion");
    private final JugadorService jugadorService;

    public JugadorController(JugadorService jugadorService) {
        this.jugadorService = jugadorService;
    }

    @Operation(summary = "Listado paginado de jugadores", description = "Devuelve el catálogo paginado, ordenado y filtrado de jugadores registrados")
    @ApiResponse(responseCode = "200", description = "Catálogo obtenido correctamente")
    @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    @GetMapping
    public JugadoresPageResponse listar(
            @Parameter(description = "Índice de página base 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de jugadores por página, entre 1 y 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Orden con formato campo,direccion; por ejemplo nombre,asc", example = "nombre,asc")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Liga exacta, por ejemplo La Liga", example = "La Liga")
            @RequestParam(required = false) String liga,
            @Parameter(description = "Texto parcial del equipo", example = "Madrid")
            @RequestParam(required = false) String equipo,
            @Parameter(description = "Texto parcial de la posición", example = "Del")
            @RequestParam(required = false) String posicion,
            @RequestParam Map<String, String> parametros) {
        if (!PARAMETROS_VALIDOS.containsAll(parametros.keySet())) {
            throw new IllegalArgumentException("El parámetro de consulta no es válido");
        }
        return jugadorService.listar(page, size, sort, liga, equipo, posicion);
    }
}
