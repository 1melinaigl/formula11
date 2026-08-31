package com.formula11.controllers;

import com.formula11.dto.PlayerResponse;
import com.formula11.services.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Players", description = "Player catalog endpoints")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/players")
    @Operation(summary = "Get all players", description = "Returns the full catalog of players for the current delivery")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Catalog returned successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<PlayerResponse>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAllPlayers());
    }
}
