package com.formula11.spike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.ResourceType;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

class WhoScoredSpikeTest {
    private static final String URL =
            "https://www.whoscored.com/matches/1994169/live/spain-laliga-2026-2027-malaga-espanyol";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final Set<String> AD_DOMAINS = Set.of(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adservice.google.com",
            "adsrvr.org",
            "criteo.com",
            "facebook.net");

    @Test
    void inspeccionaMatchCentreData() throws Exception {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                     new BrowserType.LaunchOptions()
                             .setChannel("chrome")
                             .setHeadless(false))) {
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(USER_AGENT)
                    .setLocale("es-AR")
                    .setViewportSize(1366, 768));
            Page page = context.newPage();
            page.route("**/*", WhoScoredSpikeTest::bloquearRecursos);
            try {
                page.navigate(URL, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.COMMIT)
                        .setTimeout(90_000));

                String json = null;
                long limite = System.nanoTime() + 90_000_000_000L;
                while (json == null && System.nanoTime() < limite) {
                    json = extraerMatchCentreData(page.content());
                    if (json == null) page.waitForTimeout(3_000);
                }
                if (json != null) {
                    JsonNode matchCentreData = new ObjectMapper().readTree(json);
                    imprimirEquipo(matchCentreData, "home");
                    imprimirEquipo(matchCentreData, "away");
                } else {
                    System.out.println("matchCentreData no encontrado durante 90 segundos.");
                }
            } finally {
                Files.createDirectories(Path.of("target"));
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(Path.of("target/whoscored.png"))
                        .setFullPage(true));
                Files.writeString(Path.of("target/whoscored.html"), page.content(), StandardCharsets.UTF_8);
                System.out.println("Título: " + page.title());
                page.waitForTimeout(10_000);
            }
            context.close();
        }
    }

    private static void bloquearRecursos(Route route) {
        ResourceType tipo = route.request().resourceType();
        String url = route.request().url().toLowerCase();
        boolean recursoPesado = tipo == ResourceType.IMAGE
                || tipo == ResourceType.FONT
                || tipo == ResourceType.MEDIA;
        boolean publicidad = AD_DOMAINS.stream().anyMatch(url::contains);
        if (recursoPesado || publicidad) route.abort();
        else route.resume();
    }

    private static void imprimirEquipo(JsonNode data, String equipo) {
        JsonNode jugadores = data.path(equipo).path("players");
        System.out.printf("%s: %d jugadores%n", equipo, jugadores.isArray() ? jugadores.size() : 0);
        if (jugadores.isArray() && !jugadores.isEmpty()) {
            JsonNode jugador = jugadores.get(0);
            System.out.printf(
                    "%s: playerId=%s, titular=%s, minutos=%s, rating=%s%n",
                    jugador.path("name").asText(),
                    jugador.path("playerId").asText(),
                    jugador.path("isFirstEleven").asBoolean(),
                    jugador.path("minutesPlayed").asInt(),
                    jugador.path("rating").asText());
        }
    }

    private static String extraerMatchCentreData(String html) {
        int variable = html.indexOf("matchCentreData");
        if (variable < 0) return null;

        int inicio = html.indexOf('=', variable);
        if (inicio < 0) return null;
        inicio = siguienteNoBlanco(html, inicio + 1);
        if (inicio >= html.length() || html.charAt(inicio) != '{') return null;

        int profundidad = 0;
        boolean string = false;
        boolean escapado = false;
        for (int i = inicio; i < html.length(); i++) {
            char actual = html.charAt(i);
            if (string) {
                if (escapado) escapado = false;
                else if (actual == '\\') escapado = true;
                else if (actual == '"') string = false;
                continue;
            }
            if (actual == '"') {
                string = true;
            } else if (actual == '{') {
                profundidad++;
            } else if (actual == '}' && --profundidad == 0) {
                return html.substring(inicio, i + 1);
            }
        }
        return null;
    }

    private static int siguienteNoBlanco(String value, int inicio) {
        while (inicio < value.length() && Character.isWhitespace(value.charAt(inicio))) inicio++;
        return inicio;
    }
}
