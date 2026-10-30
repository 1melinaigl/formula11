package com.formula11.controller;

import com.formula11.dto.CotizacionResponse;
import com.formula11.dto.HistorialCotizacionesResponse;
import com.formula11.dto.JugadorDetalleResponse;
import com.formula11.dto.RankingPageResponse;
import com.formula11.service.CotizacionConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Endpoints de lectura de la feature 008: detalle de jugador, ranking, historial y cotización a una fecha. */
@RestController
@Validated
@RequestMapping("/api/jugadores")
public class CotizacionJugadorController {
    private static final String ROL_SUPERUSUARIO = "ROLE_SUPERUSUARIO";
    private static final String DESCRIPCION_ESTRATEGIA =
            "Estrategia a consultar. Por defecto, la activa. Solo el superusuario puede pedir otra.";

    private final CotizacionConsultaService service;

    public CotizacionJugadorController(CotizacionConsultaService service) {
        this.service = service;
    }

    @GetMapping("/ranking")
    @Operation(summary = "Ranking de jugadores según la estrategia activa",
            description = "Ordena por el score de la última cotización, de mayor a menor.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Página del ranking, posiblemente vacía si todavía no hubo recálculo")
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    @ApiResponse(responseCode = "403", description = "Solo el superusuario puede consultar otra estrategia")
    public RankingPageResponse ranking(
            @Parameter(description = DESCRIPCION_ESTRATEGIA) @RequestParam(required = false) String estrategia,
            @Parameter(description = "Liga con su nombre legible, por ejemplo La Liga") @RequestParam(required = false) String liga,
            @Parameter(description = "POR, DEF, MED o DEL") @RequestParam(required = false) String posicion,
            @Parameter(description = "Índice de página base 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página, entre 1 y 100") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return RankingPageResponse.from(
                service.ranking(estrategia, liga, posicion, page, size, esSuperusuario(authentication)));
    }

    @GetMapping("/{jugadorId}")
    @Operation(summary = "Detalle de un jugador con su cotización actual")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Detalle del jugador")
    @ApiResponse(responseCode = "400", description = "Identificador inválido")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    @ApiResponse(responseCode = "404", description = "El jugador no existe")
    public JugadorDetalleResponse detalle(@PathVariable @Positive Long jugadorId) {
        return JugadorDetalleResponse.from(service.detalle(jugadorId));
    }

    @GetMapping("/{jugadorId}/cotizaciones")
    @Operation(summary = "Historial de cotizaciones de un jugador",
            description = "Una cotización por semana, ordenadas por fecha ascendente.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Historial, posiblemente vacío")
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    @ApiResponse(responseCode = "403", description = "Solo el superusuario puede consultar otra estrategia")
    @ApiResponse(responseCode = "404", description = "El jugador no existe")
    public HistorialCotizacionesResponse historial(
            @PathVariable @Positive Long jugadorId,
            @Parameter(description = DESCRIPCION_ESTRATEGIA) @RequestParam(required = false) String estrategia,
            @Parameter(description = "Fecha inicial inclusive") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Fecha final inclusive") @RequestParam(required = false) LocalDate hasta,
            Authentication authentication) {
        return HistorialCotizacionesResponse.from(
                service.historial(jugadorId, estrategia, desde, hasta, esSuperusuario(authentication)));
    }

    @GetMapping("/{jugadorId}/cotizaciones/vigente")
    @Operation(summary = "Cotización de un jugador a una fecha",
            description = "Devuelve la última cotización con fecha menor o igual a la pedida. Sin fecha, usa la fecha actual.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Cotización vigente a esa fecha")
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    @ApiResponse(responseCode = "401", description = "Autenticación requerida")
    @ApiResponse(responseCode = "403", description = "Solo el superusuario puede consultar otra estrategia")
    @ApiResponse(responseCode = "404", description = "El jugador no existe o no tiene cotizaciones hasta esa fecha")
    public CotizacionResponse vigente(
            @PathVariable @Positive Long jugadorId,
            @Parameter(description = DESCRIPCION_ESTRATEGIA) @RequestParam(required = false) String estrategia,
            @Parameter(description = "Fecha a consultar. Por defecto, hoy") @RequestParam(required = false) LocalDate fecha,
            Authentication authentication) {
        return CotizacionResponse.from(
                service.vigente(jugadorId, estrategia, fecha, esSuperusuario(authentication)));
    }

    static boolean esSuperusuario(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(autoridad -> ROL_SUPERUSUARIO.equals(autoridad.getAuthority()));
    }
}
