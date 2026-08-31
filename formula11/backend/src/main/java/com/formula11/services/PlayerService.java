package com.formula11.services;

import com.formula11.dto.PlayerResponse;
import com.formula11.models.Player;
import com.formula11.repositories.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<PlayerResponse> getAllPlayers() {
        return playerRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PlayerResponse toResponse(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .name(player.getName())
                .league(player.getLeague())
                .team(player.getTeam())
                .position(player.getPosition())
                .baseValue(player.getBaseValue())
                .build();
    }
}
