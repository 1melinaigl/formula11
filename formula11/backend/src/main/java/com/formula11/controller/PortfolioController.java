package com.formula11.controller;

import com.formula11.dto.PortfolioResponse;
import com.formula11.service.PortfolioService;
import com.formula11.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}
