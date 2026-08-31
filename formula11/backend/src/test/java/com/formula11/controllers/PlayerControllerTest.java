package com.formula11.controllers;

import com.formula11.dto.PlayerResponse;
import com.formula11.services.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerControllerTest {

    @Test
    void shouldReturnPlayersCatalog() {
        PlayerService playerService = Mockito.mock(PlayerService.class);
        PlayerController controller = new PlayerController(playerService);

        PlayerResponse player = PlayerResponse.builder()
                .id(1L)
                .name("Lionel Messi")
                .league("LaLiga")
                .team("Inter Miami")
                .position("Forward")
                .baseValue(new BigDecimal("100000000.00"))
                .build();

        Mockito.when(playerService.getAllPlayers()).thenReturn(List.of(player));

        ResponseEntity<List<PlayerResponse>> result = controller.getAllPlayers();

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Lionel Messi", result.getBody().get(0).getName());
    }
}
