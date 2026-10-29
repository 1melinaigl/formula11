package com.formula11.model.whoscored;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class MatchCentreParser {
    private static final String PLAYER_ID = "playerId";
    private static final String DISPLAY_NAME = "displayName";
    private static final String EVENTS = "events";
    private static final String STATS = "stats";
    private static final String PLAYERS = "players";
    private static final String EVENT_TYPE = "type";
    private static final String QUALIFIERS = "qualifiers";
    private static final String GOAL = "Goal";
    private static final String CARD = "Card";
    private static final String YELLOW = "Yellow";
    private static final String RED = "Red";
    private static final String SECOND_YELLOW = "SecondYellow";
    private static final String OWN_GOAL = "OwnGoal";
    private static final String GOAL_OWN = "goalOwn";
    private static final String ASSIST = "assist";
    private static final String GOALKEEPER = "GK";
    private static final String EXPANDED_MAX_MINUTE = "expandedMaxMinute";
    private static final String IS_FIRST_ELEVEN = "isFirstEleven";
    private static final String SUBBED_IN = "subbedInExpandedMinute";
    private static final String SUBBED_OUT = "subbedOutExpandedMinute";
    private static final String RATINGS = "ratings";
    private static final String TEAM_ID = "teamId";
    private static final String POSITION = "position";
    private static final String NAME = "name";
    private static final String SATISFIED_EVENTS_TYPES = "satisfiedEventsTypes";
    private static final String EXPANDED_MINUTE = "expandedMinute";
    private static final String SHOTS_TOTAL = "shotsTotal";
    private static final String PASSES_KEY = "passesKey";
    private static final String DRIBBLES_WON = "dribblesWon";
    private static final String TACKLE_SUCCESSFUL = "tackleSuccessful";
    private static final String INTERCEPTIONS = "interceptions";
    private static final String TOTAL_SAVES = "totalSaves";
    private final ObjectMapper objectMapper;

    public MatchCentreParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public Partido parse(String matchCentreDataJson, String eventTypesJson) {
        try {
            JsonNode data = objectMapper.readTree(matchCentreDataJson);
            JsonNode eventTypes = objectMapper.readTree(eventTypesJson);
            int expandedMaxMinute = data.path(EXPANDED_MAX_MINUTE).asInt();
            List<Jugador> jugadores = new ArrayList<>();
            Map<Long, JsonNode> playerNodes = new HashMap<>();
            for (String side : List.of("home", "away")) {
                JsonNode team = data.path(side);
                for (JsonNode player : team.path(PLAYERS)) {
                    JsonNode stats = player.path(STATS);
                    if (!stats.isObject() || stats.size() == 0) continue;
                    long playerId = player.path(PLAYER_ID).asLong();
                    playerNodes.put(playerId, player);
                    jugadores.add(parseJugador(player, team, expandedMaxMinute, data.path(EVENTS), eventTypes));
                }
            }
            return new Partido(List.copyOf(jugadores),
                    countEvents(data.path(EVENTS), GOAL, null),
                    countAssists(data.path(EVENTS), eventTypes),
                    countCards(data.path(EVENTS), YELLOW));
        } catch (IOException exception) {
            throw new IllegalArgumentException("JSON de matchCentreData inválido", exception);
        }
    }

    private Jugador parseJugador(JsonNode player, JsonNode team, int maxMinute,
                                 JsonNode events, JsonNode eventTypes) {
        JsonNode stats = player.path(STATS);
        boolean titular = player.path(IS_FIRST_ELEVEN).asBoolean(false);
        int inicio = titular ? 0 : player.path(SUBBED_IN).asInt(0);
        int fin = player.has(SUBBED_OUT)
                ? player.path(SUBBED_OUT).asInt()
                : maxMinute;
        int minutos = Math.max(0, fin - inicio);
        BigDecimal rating = ratingAtLatestMinute(stats.path(RATINGS));
        String teamId = team.path(TEAM_ID).asText();
        long playerId = player.path(PLAYER_ID).asLong();
        int goles = countPlayerGoals(events, playerId);
        int asistencias = countPlayerAssists(events, eventTypes, playerId);
        int amarillas = countPlayerCards(events, playerId, YELLOW);
        int rojas = countPlayerCards(events, playerId, RED)
                + countPlayerCards(events, playerId, SECOND_YELLOW);
        int golesRecibidos = player.path(POSITION).asText().equals(GOALKEEPER)
                ? countGoalsReceived(events, teamId, inicio, fin) : 0;
        int vallaInvicta = player.path(POSITION).asText().equals(GOALKEEPER)
                && golesRecibidos == 0 && minutos >= 60 ? 1 : 0;
        return new Jugador(playerId, player.path(NAME).asText(),
                team.path(NAME).asText(), titular, minutos, sum(stats, SHOTS_TOTAL),
                sum(stats, PASSES_KEY), sum(stats, DRIBBLES_WON), sum(stats, TACKLE_SUCCESSFUL),
                sum(stats, INTERCEPTIONS), sum(stats, TOTAL_SAVES), rating, goles,
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
            if (type.equals(event.path(EVENT_TYPE).path(DISPLAY_NAME).asText())
                    && (playerId == null || event.path(PLAYER_ID).asLong() == playerId)
                    && !hasQualifier(event, OWN_GOAL) && !hasQualifier(event, GOAL_OWN)) count++;
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
        for (JsonNode event : events) if (event.path(PLAYER_ID).asLong() == playerId && isAssist(event, eventTypes)) count++;
        return count;
    }

    private static boolean isAssist(JsonNode event, JsonNode eventTypes) {
        int assistCode = eventTypes.path(ASSIST).asInt(-1);
        for (JsonNode satisfied : event.path(SATISFIED_EVENTS_TYPES)) {
            if (satisfied.asInt(-2) == assistCode) return true;
        }
        return false;
    }

    private static int countCards(JsonNode events, String card) {
        int count = 0;
        for (JsonNode event : events) if (CARD.equals(event.path(EVENT_TYPE).path(DISPLAY_NAME).asText())
                && hasQualifier(event, card)) count++;
        return count;
    }

    private static int countPlayerCards(JsonNode events, long playerId, String card) {
        int count = 0;
        for (JsonNode event : events) if (event.path(PLAYER_ID).asLong() == playerId
                && CARD.equals(event.path(EVENT_TYPE).path(DISPLAY_NAME).asText())
                && hasQualifier(event, card)) count++;
        return count;
    }

    private static boolean hasQualifier(JsonNode event, String name) {
        for (JsonNode qualifier : event.path(QUALIFIERS)) {
            if (name.equals(qualifier.path(EVENT_TYPE).path(DISPLAY_NAME).asText())) return true;
        }
        return false;
    }

    private static int countGoalsReceived(JsonNode events, String teamId, int start, int end) {
        int count = 0;
        for (JsonNode event : events) {
            if (GOAL.equals(event.path(EVENT_TYPE).path(DISPLAY_NAME).asText())
                    && !teamId.equals(event.path(TEAM_ID).asText())
                    && event.path(EXPANDED_MINUTE).asInt() >= start
                    && event.path(EXPANDED_MINUTE).asInt() <= end
                    && !hasQualifier(event, OWN_GOAL) && !hasQualifier(event, GOAL_OWN)) count++;
        }
        return count;
    }

    public record Partido(List<Jugador> jugadores, int goles, int asistencias, int amarillas) {}

    public record Jugador(long playerId, String nombre, String equipo, boolean titular, int minutos,
                          int tiros, int pasesClave, int regates, int entradas, int intercepciones,
                          int atajadas, BigDecimal rating, int goles, int asistencias, int amarillas,
                          int rojas, int golesRecibidos, int vallaInvicta) {}
}
