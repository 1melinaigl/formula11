package com.formula11.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.formula11.config.ClockConfig;
import com.formula11.config.WhoScoredProperties;
import com.formula11.model.Liga;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WhoScoredPlaywrightAdapter implements WhoScoredPort, AutoCloseable {
    private static final String BASE_URL = "https://www.whoscored.com";
    /** El calendario de WhoScored pide el mes como yyyyMM (por ejemplo, 202610). */
    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("yyyyMM");
    private static final double INTERVALO_SONDEO_MS = 1000;
    private final WhoScoredProperties properties;
    private final ObjectMapper objectMapper;
    private Playwright playwright;
    private BrowserContext context;

    public WhoScoredPlaywrightAdapter(WhoScoredProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public synchronized void abrirCarga() {
        if (context != null) return;
        playwright = Playwright.create();
        BrowserType.LaunchPersistentContextOptions options =
                new BrowserType.LaunchPersistentContextOptions()
                        .setChannel("chrome")
                        .setHeadless(properties.isHeadless())
                        .setLocale("es-AR")
                        .setViewportSize(1366, 768)
                        .setIgnoreDefaultArgs(List.of("--enable-automation"))
                        .setArgs(List.of("--disable-blink-features=AutomationControlled"));
        context = playwright.chromium().launchPersistentContext(Path.of(properties.getPerfilDir()), options);
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");
    }

    @Override
    public List<PartidoWhoScoredResumen> listarPartidos(Liga liga, YearMonth mes) {
        abrirCarga();
        Page page = context.newPage();
        try {
            navegar(page, BASE_URL);
            String url = "/tournaments/" + properties.getLigas().get(liga)
                    + "/data/?d=" + mes.format(FORMATO_MES) + "&isAggregate=false";
            String json = evaluarConReintentos(page,
                    "() => fetch('" + url + "').then(response => response.text())");
            return new CalendarioWhoScoredParser(objectMapper).parse(json, liga,
                    ZoneId.of(properties.getZonaFuente()), ClockConfig.ZONA_NEGOCIO);
        } finally {
            page.close();
        }
    }

    @Override
    public DatosPartidoWhoScored obtenerDatos(long partidoId) {
        abrirCarga();
        Page page = context.newPage();
        try {
            navegar(page, BASE_URL + "/Matches/" + partidoId + "/Live");
            String data = evaluarConReintentos(page,
                    "() => JSON.stringify(require.config.params.args.matchCentreData)");
            String types = evaluarConReintentos(page,
                    "() => JSON.stringify(require.config.params.args.matchCentreEventTypeJson)");
            JsonNode parsed = read(data);
            ZonedDateTime start = LocalDateTime.parse(parsed.path("startTime").asText())
                    .atZone(ZoneId.of(properties.getZonaFuente()))
                    .withZoneSameInstant(ClockConfig.ZONA_NEGOCIO);
            // Pausa entre partidos para no saturar a WhoScored.
            page.waitForTimeout(properties.getPausaMs());
            return new DatosPartidoWhoScored(partidoId, null, start, data, types);
        } finally {
            page.close();
        }
    }

    private void navegar(Page page, String url) {
        page.navigate(url, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.COMMIT)
                .setTimeout(properties.getTimeoutMs()));
    }

    /**
     * Sondea la página hasta que la expresión devuelva datos o se agote app.whoscored.timeout-ms.
     * La página de WhoScored sigue navegando después de cargar, así que los errores intermedios se ignoran.
     */
    private String evaluarConReintentos(Page page, String expression) {
        int intentos = Math.max(properties.getReintentos(),
                (int) Math.ceil(properties.getTimeoutMs() / INTERVALO_SONDEO_MS));
        RuntimeException last = null;
        for (int intento = 0; intento < intentos; intento++) {
            try {
                Object value = page.evaluate(expression);
                if (value instanceof String text && !text.isBlank() && !"null".equals(text)) return text;
            } catch (PlaywrightException exception) {
                last = exception;
            }
            page.waitForTimeout(INTERVALO_SONDEO_MS);
        }
        throw new IllegalStateException("WhoScored no entregó los datos JavaScript", last);
    }

    private JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON de WhoScored inválido", exception);
        }
    }

    @Override
    public void finalizarCarga() {
        close();
    }

    @Override
    public synchronized void close() {
        if (context != null) context.close();
        context = null;
        if (playwright != null) playwright.close();
        playwright = null;
    }
}
