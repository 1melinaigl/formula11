package com.formula11.controller;

import com.formula11.dto.OperacionResponse;
import com.formula11.dto.OrdenRequest;
import com.formula11.service.MercadoService;
import com.formula11.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ordenes")
public class MercadoController {
    private final MercadoService mercadoService;
    private final UsuarioService usuarioService;

    public MercadoController(MercadoService mercadoService, UsuarioService usuarioService) {
        this.mercadoService = mercadoService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/compra")
    @ResponseStatus(HttpStatus.CREATED)
    public OperacionResponse comprar(@Valid @RequestBody OrdenRequest request, Authentication authentication) {
        Long usuarioId = usuarioService.obtenerIdPorEmail(authentication.getName());
        return OperacionResponse.from(mercadoService.comprar(usuarioId, request.jugadorId(), request.cantidad()));
    }

    @PostMapping("/venta")
    @ResponseStatus(HttpStatus.CREATED)
    public OperacionResponse vender(@Valid @RequestBody OrdenRequest request, Authentication authentication) {
        Long usuarioId = usuarioService.obtenerIdPorEmail(authentication.getName());
        return OperacionResponse.from(mercadoService.vender(usuarioId, request.jugadorId(), request.cantidad()));
    }
}
