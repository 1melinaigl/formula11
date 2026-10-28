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

/**
 * Parsea la respuesta de /tournaments/{stageId}/data/?d={yyyyMM}&isAggregate=false.
 * Formato verificado en una respuesta real: tournaments[].matches[] con id, startTime (sin zona,
 * en hora de la fuente), homeTeamName, awayTeamName y elapsed ("FT" cuando el partido terminó).
 */
public final class CalendarioWhoScoredParser {
    private static final String START_TIME = "startTime";
    private final ObjectMapper objectMapper;

    public CalendarioWhoScoredParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public List<PartidoWhoScoredResumen> parse(String json, Liga liga,
                                                ZoneId zonaFuente, ZoneId zonaNegocio) {
        JsonNode root = leer(json);
        List<PartidoWhoScoredResumen> result = new ArrayList<>();
        for (JsonNode tournament : root.path("tournaments")) {
            for (JsonNode match : tournament.path("matches")) {
                result.add(partido(match, liga, zonaFuente, zonaNegocio));
            }
        }
        return List.copyOf(result);
    }

    private JsonNode leer(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Respuesta de calendario WhoScored inválida", exception);
        }
    }

    private static PartidoWhoScoredResumen partido(JsonNode match, Liga liga,
                                                   ZoneId zonaFuente, ZoneId zonaNegocio) {
        long id = match.path("id").asLong(-1);
        if (id <= 0) throw new IllegalArgumentException("Partido de calendario sin id");
        ZonedDateTime fecha = LocalDateTime.parse(texto(match, START_TIME))
                .atZone(zonaFuente).withZoneSameInstant(zonaNegocio);
        return new PartidoWhoScoredResumen(id, fecha, texto(match, "homeTeamName"),
                texto(match, "awayTeamName"), match.path("elapsed").asText(""), liga);
    }

    private static String texto(JsonNode node, String key) {
        String value = node.path(key).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Partido de calendario sin " + key);
        }
        return value;
    }
}
