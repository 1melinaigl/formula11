package com.formula11.model.whoscored;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class MatchCentreParser {
    private final ObjectMapper objectMapper;

    public MatchCentreParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public Partido parse(String matchCentreDataJson, String eventTypesJson) {
        try {
            JsonNode data = objectMapper.readTree(matchCentreDataJson);
            JsonNode eventTypes = objectMapper.readTree(eventTypesJson);
            int expandedMaxMinute = data.path("expandedMaxMinute").asInt();
            List<Jugador> jugadores = new ArrayList<>();
            Map<Long, JsonNode> playerNodes = new HashMap<>();
            for (String side : List.of("home", "away")) {
                JsonNode team = data.path(side);
                for (JsonNode player : team.path("players")) {
                    JsonNode stats = player.path("stats");
                    if (!stats.isObject() || stats.size() == 0) continue;
                    long playerId = player.path("playerId").asLong();
                    playerNodes.put(playerId, player);
                    jugadores.add(parseJugador(player, team, expandedMaxMinute, data.path("events"), eventTypes));
                }
            }
            return new Partido(List.copyOf(jugadores),
                    countEvents(data.path("events"), "Goal", null),
                    countAssists(data.path("events"), eventTypes),
                    countCards(data.path("events"), "Yellow"));
        } catch (IOException exception) {
            throw new IllegalArgumentException("JSON de matchCentreData inválido", exception);
        }
    }

    private Jugador parseJugador(JsonNode player, JsonNode team, int maxMinute,
                                 JsonNode events, JsonNode eventTypes) {
        JsonNode stats = player.path("stats");
        boolean titular = player.path("isFirstEleven").asBoolean(false);
        int inicio = titular ? 0 : player.path("subbedInExpandedMinute").asInt(0);
        int fin = player.has("subbedOutExpandedMinute")
                ? player.path("subbedOutExpandedMinute").asInt()
                : maxMinute;
        int minutos = Math.max(0, fin - inicio);
        BigDecimal rating = ratingAtLatestMinute(stats.path("ratings"));
        String teamId = team.path("teamId").asText();
        int goles = countPlayerGoals(events, player.path("playerId").asLong());
        int asistencias = countPlayerAssists(events, eventTypes, player.path("playerId").asLong());
        int amarillas = countPlayerCards(events, player.path("playerId").asLong(), "Yellow");
        int rojas = countPlayerCards(events, player.path("playerId").asLong(), "Red")
                + countPlayerCards(events, player.path("playerId").asLong(), "SecondYellow");
        int golesRecibidos = player.path("position").asText().equals("GK")
                ? countGoalsReceived(events, teamId, inicio, fin) : 0;
        int vallaInvicta = player.path("position").asText().equals("GK")
                && golesRecibidos == 0 && minutos >= 60 ? 1 : 0;
        return new Jugador(player.path("playerId").asLong(), player.path("name").asText(),
                team.path("name").asText(), titular, minutos, sum(stats, "shotsTotal"),
                sum(stats, "passesKey"), sum(stats, "dribblesWon"), sum(stats, "tackleSuccessful"),
                sum(stats, "interceptions"), sum(stats, "totalSaves"), rating, goles,
                asistencias, amarillas, rojas, golesRecibidos, vallaInvicta);
    }

    private static BigDecimal ratingAtLatestMinute(JsonNode ratings) {
        if (!ratings.isObject() || ratings.size() == 0) return null;
        int latest = ratings.fieldNames().hasNext() ? Integer.MIN_VALUE : 0;
        Iterator<String> fields = ratings.fieldNames();
        while (fields.hasNext()) latest = Math.max(latest, Integer.parseInt(fields.next()));
        return ratings.path(String.valueOf(latest)).decimalValue().setScale(2, RoundingMode.HALF_UP);
    }

    private static int sum(JsonNode stats, String key) {
        int total = 0;
        JsonNode values = stats.path(key);
        if (values.isObject()) {
            for (JsonNode value : values) total += value.asDouble() >= 0 ? (int) value.asDouble() : 0;
        }
        return total;
    }

    private static int countEvents(JsonNode events, String type, Long playerId) {
        int count = 0;
        for (JsonNode event : events) {
            if (type.equals(event.path("type").path("displayName").asText())
                    && (playerId == null || event.path("playerId").asLong() == playerId)
                    && !hasQualifier(event, "OwnGoal") && !hasQualifier(event, "goalOwn")) count++;
        }
        return count;
    }

    private static int countPlayerGoals(JsonNode events, long playerId) {
        return countEvents(events, "Goal", playerId);
    }

    private static int countAssists(JsonNode events, JsonNode eventTypes) {
        return (int) java.util.stream.StreamSupport.stream(events.spliterator(), false)
                .filter(event -> isAssist(event, eventTypes)).count();
    }

    private static int countPlayerAssists(JsonNode events, JsonNode eventTypes, long playerId) {
        int count = 0;
        for (JsonNode event : events) if (event.path("playerId").asLong() == playerId && isAssist(event, eventTypes)) count++;
        return count;
    }

    private static boolean isAssist(JsonNode event, JsonNode eventTypes) {
        int assistCode = eventTypes.path("assist").asInt(-1);
        for (JsonNode satisfied : event.path("satisfiedEventsTypes")) {
            if (satisfied.asInt(-2) == assistCode) return true;
        }
        return false;
    }

    private static int countCards(JsonNode events, String card) {
        int count = 0;
        for (JsonNode event : events) if ("Card".equals(event.path("type").path("displayName").asText())
                && hasQualifier(event, card)) count++;
        return count;
    }

    private static int countPlayerCards(JsonNode events, long playerId, String card) {
        int count = 0;
        for (JsonNode event : events) if (event.path("playerId").asLong() == playerId
                && "Card".equals(event.path("type").path("displayName").asText())
                && hasQualifier(event, card)) count++;
        return count;
    }

    private static boolean hasQualifier(JsonNode event, String name) {
        for (JsonNode qualifier : event.path("qualifiers")) {
            if (name.equals(qualifier.path("type").path("displayName").asText())) return true;
        }
        return false;
    }

    private static int countGoalsReceived(JsonNode events, String teamId, int start, int end) {
        int count = 0;
        for (JsonNode event : events) {
            if ("Goal".equals(event.path("type").path("displayName").asText())
                    && !teamId.equals(event.path("teamId").asText())
                    && event.path("expandedMinute").asInt() >= start
                    && event.path("expandedMinute").asInt() <= end
                    && !hasQualifier(event, "OwnGoal") && !hasQualifier(event, "goalOwn")) count++;
        }
        return count;
    }

    public record Partido(List<Jugador> jugadores, int goles, int asistencias, int amarillas) {}

    public record Jugador(long playerId, String nombre, String equipo, boolean titular, int minutos,
                          int tiros, int pasesClave, int regates, int entradas, int intercepciones,
                          int atajadas, BigDecimal rating, int goles, int asistencias, int amarillas,
                          int rojas, int golesRecibidos, int vallaInvicta) {}
}
