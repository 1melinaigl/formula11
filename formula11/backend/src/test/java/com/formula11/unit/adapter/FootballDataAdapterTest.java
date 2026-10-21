package com.formula11.adapter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.formula11.model.Liga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FootballDataAdapterTest {
    private FootballApiProperties properties;
    private RestClient restClient;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        properties = new FootballApiProperties();
        properties.setApiKey("test-key");
        restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec request = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headers = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(request);
        when(request.uri(anyString(), anyMap())).thenReturn(headers);
        when(headers.header(anyString(), any(String[].class))).thenReturn(headers);
        when(headers.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void apikeyAusenteNoIntentaLlamarAlProveedor() {
        properties.setApiKey("  ");
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build());

        FootballDataException exception = assertThrows(FootballDataException.class, () -> adapter.obtenerEquipos(Liga.LA_LIGA));

        assertEquals(FootballDataException.Tipo.NO_CONFIGURADO, exception.getTipo());
    }

    @Test
    void mapeaEquiposYJugadoresYUsaCache() throws Exception {
        Object player = record("PlayerPayload", 10L, "Forward", "midfield");
        Object team = record("TeamPayload", 20L, " Team ", List.of(player));
        Object response = record("TeamsResponse", List.of(team));
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(response);

        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build(), restClient);

        List<ExternalTeam> teams = adapter.obtenerEquipos(Liga.LA_LIGA);
        List<ExternalPlayer> players = adapter.obtenerJugadores(teams.get(0));

        assertEquals(List.of(new ExternalTeam("20", "Team", "LA_LIGA")), teams);
        assertEquals(List.of(new ExternalPlayer("10", "Forward", "Team", "LA_LIGA", "MED")), players);
    }

    @Test
    void respuestaNulaSeTraduceARespuestaInvalida() {
        stubResponse(null);
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build(), restClient);

        FootballDataException exception = assertThrows(FootballDataException.class,
                () -> adapter.obtenerEquipos(Liga.LA_LIGA));

        assertEquals(FootballDataException.Tipo.RESPUESTA_INVALIDA, exception.getTipo());
    }

    @Test
    void mapeaJugadoresDesdeDetalleDelEquipo() throws Exception {
        Object player = record("PlayerPayload", 10L, "Defender", "defender");
        stubResponse(record("TeamDetailResponse", List.of(player)));
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build(), restClient);

        List<ExternalPlayer> players = adapter.obtenerJugadores(new ExternalTeam("20", "Team", Liga.LA_LIGA.name()));

        assertEquals("DEF", players.get(0).posicion());
    }

    @Test
    void traduceErroresDeDisponibilidadYRespuestaHttp() {
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build(), restClient);
        doThrow(new ResourceAccessException("timeout")).when(restClient).get();

        FootballDataException timeout = assertThrows(FootballDataException.class,
                () -> adapter.obtenerEquipos(Liga.LA_LIGA));
        assertEquals(FootballDataException.Tipo.TIMEOUT, timeout.getTipo());

        doThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "rate", null, null, null))
                .when(restClient).get();
        FootballDataException limit = assertThrows(FootballDataException.class,
                () -> adapter.obtenerEquipos(Liga.LA_LIGA));
        assertEquals(FootballDataException.Tipo.LIMITE, limit.getTipo());
    }

    @Test
    void rechazaLigaSinCodigoExterno() {
        properties.setCompetitionCodes(java.util.Map.of());
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build(), restClient);

        FootballDataException exception = assertThrows(FootballDataException.class,
                () -> adapter.obtenerEquipos(Liga.LA_LIGA));

        assertEquals(FootballDataException.Tipo.RESPUESTA_INVALIDA, exception.getTipo());
    }

    @Test
    void errorDelProveedorSeRelanzaConContextoSinDuplicarLog() {
        FootballDataException original = new FootballDataException(FootballDataException.Tipo.PROVEEDOR, "fallo");
        doThrow(original).when(restClient).get();
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build(), restClient);

        FootballDataException exception = assertThrows(FootballDataException.class,
                () -> adapter.obtenerEquipos(Liga.LA_LIGA));

        assertTrue(exception.getMessage().contains("teams"));
        assertEquals(original, exception.getCause());
    }

    @Test
    void respetaElIntervaloMinimoEntreSolicitudes() throws Exception {
        properties.setMinimumRequestInterval(Duration.ofMillis(1));
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build(), restClient);
        Field nextRequest = FootballDataAdapter.class.getDeclaredField("nextRequestNanos");
        nextRequest.setAccessible(true);
        nextRequest.setLong(adapter, System.nanoTime() + Duration.ofMillis(20).toNanos());

        long inicio = System.nanoTime();
        invokeRateLimit(adapter);

        assertTrue(System.nanoTime() - inicio >= Duration.ofMillis(5).toNanos());
    }

    private static void invokeRateLimit(FootballDataAdapter adapter) throws Exception {
        var method = FootballDataAdapter.class.getDeclaredMethod("esperarRateLimit");
        method.setAccessible(true);
        method.invoke(adapter);
    }

    private void stubResponse(Object response) {
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(response);
    }

    private static Object record(String name, Object... arguments) throws Exception {
        Class<?> type = Class.forName(FootballDataAdapter.class.getName() + "$" + name);
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance(arguments);
    }
}
