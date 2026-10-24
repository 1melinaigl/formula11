package com.formula11.controller;

import com.formula11.dto.MercadoJugadorResponse;
import com.formula11.service.MercadoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jugadores")
public class MercadoJugadorController {
    private final MercadoService mercadoService;

    public MercadoJugadorController(MercadoService mercadoService) {
        this.mercadoService = mercadoService;
    }

    @GetMapping("/{jugadorId}/mercado")
    public MercadoJugadorResponse consultarMercado(@PathVariable Long jugadorId) {
        return MercadoJugadorResponse.from(mercadoService.consultarMercado(jugadorId));
    }
}
