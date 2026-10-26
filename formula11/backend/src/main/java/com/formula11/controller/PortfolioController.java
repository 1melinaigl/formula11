package com.formula11.controller;

import com.formula11.dto.PortfolioResponse;
import com.formula11.dto.EvolucionPortfolioResponse;
import com.formula11.dto.TransaccionesPageResponse;
import com.formula11.model.Transaccion;
import com.formula11.service.PuntoEvolucionResultado;
import java.time.LocalDate;
import java.util.List;
import com.formula11.service.PortfolioService;
import com.formula11.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/usuarios/{usuarioId}")
public class PortfolioController {
    private final PortfolioService portfolioService;
    private final UsuarioService usuarioService;

    public PortfolioController(PortfolioService portfolioService, UsuarioService usuarioService) {
        this.portfolioService = portfolioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/portfolio")
    public PortfolioResponse portfolio(@PathVariable Long usuarioId, Authentication authentication) {
        Long solicitanteId = usuarioService.obtenerIdPorEmail(authentication.getName());
        return PortfolioResponse.from(portfolioService.portfolio(solicitanteId, usuarioId));
    }

    @GetMapping("/portfolio/evolucion")
    public EvolucionPortfolioResponse evolucion(@PathVariable Long usuarioId,
                                                @org.springframework.web.bind.annotation.RequestParam(required = false) LocalDate desde,
                                                @org.springframework.web.bind.annotation.RequestParam(required = false) LocalDate hasta,
                                                Authentication authentication) {
        Long solicitanteId = usuarioService.obtenerIdPorEmail(authentication.getName());
        var resultado = portfolioService.evolucion(solicitanteId, usuarioId, desde, hasta);
        List<PuntoEvolucionResultado> puntos = resultado.puntos();
        return new EvolucionPortfolioResponse(
                resultado.usuario().getId(),
                puntos.stream().map(com.formula11.dto.PuntoEvolucionResponse::from).toList());
    }

    @GetMapping("/transacciones")
    public TransaccionesPageResponse historial(
            @PathVariable Long usuarioId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0")
            @jakarta.validation.constraints.Min(0) int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20")
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Transaccion.Tipo tipo,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @jakarta.validation.constraints.Positive Long jugadorId,
            Authentication authentication) {
        Long solicitanteId = usuarioService.obtenerIdPorEmail(authentication.getName());
        return TransaccionesPageResponse.from(
                portfolioService.historial(solicitanteId, usuarioId, tipo, jugadorId, page, size));
    }
}
