package com.formula11.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.model.Liga;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CalendarioWhoScoredParser {
    private final ObjectMapper objectMapper;

    public CalendarioWhoScoredParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public List<PartidoWhoScoredResumen> parse(String json, Liga liga,
                                                ZoneId zonaFuente, ZoneId zonaNegocio) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<PartidoWhoScoredResumen> result = new ArrayList<>();
            for (JsonNode tournament : root.path("tournaments")) {
                for (JsonNode match : tournament.path("matches")) {
                    String startTime = text(match, "startTime");
                    ZonedDateTime fecha = LocalDateTime.parse(startTime)
                            .atZone(zonaFuente).withZoneSameInstant(zonaNegocio);
                    JsonNode status = match.path("status");
                    String elapsed = status.isTextual() ? status.asText()
                            : firstText(status, "name", "displayName", "type");
                    result.add(new PartidoWhoScoredResumen(match.path("id").asLong(), fecha,
                            teamName(match, "homeTeam"), teamName(match, "awayTeam"),
                            elapsed, liga));
                }
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Respuesta de calendario WhoScored inválida", exception);
        }
    }

    private static String teamName(JsonNode match, String key) {
        JsonNode team = match.path(key);
        return team.isTextual() ? team.asText() : text(team, "name");
    }

    private static String text(JsonNode node, String key) {
        String value = node.path(key).asText(null);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + key);
        return value;
    }

    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = node.path(key).asText(null);
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
