package com.formula11.adapter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.formula11.model.Liga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class FootballDataAdapter implements FootballDataPort {
    private static final Logger log = LoggerFactory.getLogger(FootballDataAdapter.class);
    private final FootballApiProperties properties;
    private final RestClient restClient;
    private final com.github.benmanes.caffeine.cache.Cache<String, Object> cache;
    private long nextRequestNanos;

    @Autowired
    public FootballDataAdapter(FootballApiProperties properties, com.github.benmanes.caffeine.cache.Cache<String, Object> cache) {
        this(properties, cache, crearRestClient(properties));
    }

    FootballDataAdapter(FootballApiProperties properties,
                        com.github.benmanes.caffeine.cache.Cache<String, Object> cache,
                        RestClient restClient) {
        this.properties = properties;
        this.cache = cache;
        this.restClient = restClient;
    }

    private static RestClient crearRestClient(FootballApiProperties properties) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(requestFactory).build();
    }

    @Override
    public boolean estaConfigurado() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalTeam> obtenerEquipos(Liga liga) {
        validarConfiguracion();
        String competitionCode = properties.getCompetitionCodes().get(liga.name());
        if (competitionCode == null || competitionCode.isBlank()) {
            throw new FootballDataException(FootballDataException.Tipo.RESPUESTA_INVALIDA, "No hay código externo configurado para la liga " + liga.getNombreLegible());
        }
        String key = "teams:" + competitionCode;
        Object cached = cache.getIfPresent(key);
        if (cached != null) {
            log.debug("football_api_cache_hit provider=football-data league={} resource=teams", liga.name());
            return (List<ExternalTeam>) cached;
        }
        TeamsResponse response = get("/competitions/{competition}/teams", Map.of("competition", competitionCode), TeamsResponse.class, liga.name(), "teams");
        if (response == null || response.teams() == null) throw invalidResponse(liga.name());
        List<ExternalTeam> teams = response.teams().stream()
        .filter(team -> team.id() != null && team.name() != null && !team.name().isBlank())
        .map(team -> {
            ExternalTeam externo = new ExternalTeam(String.valueOf(team.id()), team.name().trim(), liga.name());
            if (team.squad() != null && !team.squad().isEmpty()) {
                cache.put("squad:" + externo.externalId(), mapearJugadores(team.squad(), externo));
            }
            return externo;
        })
                .toList();
        cache.put(key, teams);
        return teams;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalPlayer> obtenerJugadores(ExternalTeam equipo) {
        validarConfiguracion();
        String key = "squad:" + equipo.externalId();
        Object cached = cache.getIfPresent(key);
        if (cached != null) {
            log.debug("football_api_cache_hit provider=football-data team={} resource=squad", equipo.externalId());
            return (List<ExternalPlayer>) cached;
        }
        TeamDetailResponse response = get("/teams/{team}", Map.of("team", equipo.externalId()), TeamDetailResponse.class, equipo.liga(), "squad");
        if (response == null || response.squad() == null) throw invalidResponse(equipo.liga());
        List<ExternalPlayer> players = mapearJugadores(response.squad(), equipo);
        cache.put(key, players);
        return players;
    }

    private <T> T get(String path, Map<String, ?> variables, Class<T> responseType, String liga, String resource) {
        esperarRateLimit();
        long inicio = System.nanoTime();
        try {
            T response = restClient.get().uri(path, variables)
                    .header(properties.getApiKeyHeader(), properties.getApiKey().trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        throw new FootballDataException(tipoPara(clientResponse.getStatusCode().value()), mensajePara(clientResponse.getStatusCode().value(), liga));
                    })
                    .body(responseType);
            log.info("football_api_call provider=football-data league={} resource={} status=success durationMs={}", liga, resource, duracionMs(inicio));
            return response;
        } catch (FootballDataException exception) {
            throw new FootballDataException(exception.getTipo(),
                    "Error al consultar " + resource + " para la liga " + liga, exception);
        } catch (RestClientResponseException exception) {
            FootballDataException translated = new FootballDataException(tipoPara(exception.getStatusCode().value()), mensajePara(exception.getStatusCode().value(), liga));
            log.warn("football_api_call provider=football-data league={} resource={} status={} durationMs={}", liga, resource, exception.getStatusCode().value(), duracionMs(inicio));
            throw translated;
        } catch (ResourceAccessException | DataBufferLimitException exception) {
            log.warn("football_api_call provider=football-data league={} resource={} status=unavailable durationMs={}", liga, resource, duracionMs(inicio));
            throw new FootballDataException(FootballDataException.Tipo.TIMEOUT, "El proveedor de fútbol no está disponible", exception);
        } catch (RuntimeException exception) {
            log.warn("football_api_call provider=football-data league={} resource={} status=invalid durationMs={}", liga, resource, duracionMs(inicio));
            throw new FootballDataException(FootballDataException.Tipo.RESPUESTA_INVALIDA, "La respuesta del proveedor de fútbol no es válida", exception);
        }
    }

    private void validarConfiguracion() {
        if (!estaConfigurado()) {
            throw new FootballDataException(FootballDataException.Tipo.NO_CONFIGURADO, "La integración de fútbol no está configurada");
        }
    }

    private void esperarRateLimit() {
        Duration intervalo = properties.getMinimumRequestInterval();
        long esperaNanos = reservarProximaSolicitud(intervalo.toNanos());
        if (esperaNanos <= 0) return;
        try {
            long millis = esperaNanos / 1_000_000;
            int nanos = (int) (esperaNanos % 1_000_000);
            Thread.sleep(millis, nanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FootballDataException(FootballDataException.Tipo.TIMEOUT, "La espera del límite del proveedor fue interrumpida", exception);
        }
    }

    private synchronized long reservarProximaSolicitud(long intervaloNanos) {
        long ahora = System.nanoTime();
        long esperaNanos = nextRequestNanos - ahora;
        nextRequestNanos = Math.max(ahora, nextRequestNanos) + intervaloNanos;
        return esperaNanos;
    }

    private static FootballDataException.Tipo tipoPara(int status) {
        if (status == 401 || status == 403) return FootballDataException.Tipo.NO_AUTORIZADO;
        if (status == 429) return FootballDataException.Tipo.LIMITE;
        return FootballDataException.Tipo.PROVEEDOR;
    }

    private static String mensajePara(int status, String liga) {
        if (status == 401 || status == 403) return "La apikey del proveedor de fútbol no es válida o no está autorizada";
        if (status == 429) return "El proveedor de fútbol alcanzó el límite de requests para " + liga;
        return "El proveedor de fútbol devolvió un error para " + liga;
    }

    private static FootballDataException invalidResponse(String liga) {
        return new FootballDataException(FootballDataException.Tipo.RESPUESTA_INVALIDA, "La respuesta de fútbol no contiene datos válidos para " + liga);
    }

    private static long duracionMs(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }

    private static String normalizarPosicion(String posicion) {
        String valor = posicion.toLowerCase(Locale.ROOT);
        if (valor.contains("goal")) return "POR";
        if (valor.contains("defen")) return "DEF";
        if (valor.contains("midfield")) return "MED";
        return "DEL";
    }

    private static List<ExternalPlayer> mapearJugadores(List<PlayerPayload> squad, ExternalTeam equipo) {
    return squad.stream()
            .filter(player -> player.id() != null && player.name() != null && player.position() != null)
            .map(player -> new ExternalPlayer(
                    String.valueOf(player.id()), player.name().trim(), equipo.nombre(), equipo.liga(), normalizarPosicion(player.position())))
            .toList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TeamsResponse(List<TeamPayload> teams) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TeamPayload(Long id, String name, List<PlayerPayload> squad) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TeamDetailResponse(List<PlayerPayload> squad) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlayerPayload(Long id, String name, String position) { }
}
