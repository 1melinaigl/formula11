package com.formula11.services;

import com.formula11.dto.PlayerResponse;
import com.formula11.models.Player;
import com.formula11.repositories.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void shouldReturnPlayerCatalog() {
        Player player = Player.builder()
                .id(1L)
                .name("Lionel Messi")
                .league("LaLiga")
                .team("Inter Miami")
                .position("Forward")
                .baseValue(new BigDecimal("100000000.00"))
                .build();

        when(playerRepository.findAll()).thenReturn(List.of(player));

        List<PlayerResponse> response = playerService.getAllPlayers();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Lionel Messi", response.get(0).getName());
        assertEquals("Forward", response.get(0).getPosition());
    }
}
