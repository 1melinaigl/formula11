package com.formula11.spike;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class WhoScoredSpikeTest {
    private static final String URL =
            "https://www.whoscored.com/matches/1994169/live/spain-laliga-2026-2027-malaga-espanyol";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    @Test
    void inspeccionaMatchCentreData() throws Exception {
        try (Playwright playwright = Playwright.create();
             BrowserContext context = playwright.chromium().launchPersistentContext(
                     Path.of("target/whoscored-perfil"),
                     new BrowserType.LaunchPersistentContextOptions()
                    .setChannel("chrome")
                    .setHeadless(false)
                    .setIgnoreDefaultArgs(List.of("--enable-automation"))
                    .setArgs(List.of("--disable-blink-features=AutomationControlled"))
                    .setUserAgent(USER_AGENT)
                    .setLocale("es-AR")
                    .setViewportSize(1366, 768))) {
            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");
            Page page = context.newPage();
            try {
                Response response = page.navigate(URL, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.COMMIT)
                        .setTimeout(90_000));

                String respuestaHtml = response.text();
                String json = extraerMatchCentreData(respuestaHtml);
                Files.createDirectories(Path.of("target"));
                Files.writeString(
                        Path.of("target/whoscored-respuesta.html"),
                        respuestaHtml,
                        StandardCharsets.UTF_8);
                if (json != null) {
                    System.out.println("Funcionó: respuesta original del servidor.");
                }

                if (json == null) {
                    json = buscarConJavaScript(page);
                    if (json != null) {
                        System.out.println("Funcionó: JavaScript con require.config.");
                    }
                }

                if (json == null) {
                    json = buscarEnContenido(page);
                    if (json != null) {
                        System.out.println("Funcionó: page.content().");
                    }
                }

                if (json != null) {
                    JsonNode matchCentreData = new ObjectMapper().readTree(json);
                    imprimirEquipo(matchCentreData, "home");
                    imprimirEquipo(matchCentreData, "away");
                } else {
                    System.out.println("matchCentreData no encontrado.");
                }
            } finally {
                try {
                    Files.createDirectories(Path.of("target"));
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(Path.of("target/whoscored.png"))
                            .setFullPage(true));
                } catch (PlaywrightException | java.io.IOException e) {
                    System.out.println("No se pudo guardar la captura: " + e.getMessage());
                }
                try {
                    Files.writeString(
                            Path.of("target/whoscored.html"),
                            page.content(),
                            StandardCharsets.UTF_8);
                } catch (PlaywrightException | java.io.IOException e) {
                    System.out.println("No se pudo guardar el HTML: " + e.getMessage());
                }
                try {
                    System.out.println("Título: " + page.title());
                } catch (PlaywrightException e) {
                    System.out.println("No se pudo obtener el título: " + e.getMessage());
                }
                page.waitForTimeout(60_000);
            }
        }
    }

    private static String buscarConJavaScript(Page page) {
        long limite = System.nanoTime() + 90_000_000_000L;
        while (System.nanoTime() < limite) {
            try {
                String json = (String) page.evaluate(
                        "() => { try { return JSON.stringify(require.config.params.args.matchCentreData); } "
                                + "catch (e) { return null; } }");
                if (json != null && !json.equals("null")) return json;
            } catch (PlaywrightException e) {
                System.out.println("No se pudo evaluar matchCentreData: " + e.getMessage());
            }
            page.waitForTimeout(3_000);
        }
        return null;
    }

    private static String buscarEnContenido(Page page) {
        long limite = System.nanoTime() + 90_000_000_000L;
        while (System.nanoTime() < limite) {
            try {
                String json = extraerMatchCentreData(page.content());
                if (json != null) return json;
            } catch (PlaywrightException e) {
                System.out.println("La página está navegando; se reintentará page.content().");
            }
            page.waitForTimeout(3_000);
        }
        return null;
    }

    private static void imprimirEquipo(JsonNode data, String equipo) {
        JsonNode jugadores = data.path(equipo).path("players");
        System.out.printf("%s: %d jugadores%n", equipo, jugadores.isArray() ? jugadores.size() : 0);
        if (jugadores.isArray() && !jugadores.isEmpty()) {
            JsonNode jugador = jugadores.get(0);
            System.out.printf(
                    "%s: playerId=%s, titular=%s, minutos=%s, rating final=%s%n",
                    jugador.path("name").asText(),
                    jugador.path("playerId").asText(),
                    jugador.path("isFirstEleven").asBoolean(),
                    minutosCalculados(jugador),
                    ratingFinal(jugador));
        }
    }

    private static int minutosCalculados(JsonNode jugador) {
        JsonNode minutos = jugador.path("minutesPlayed");
        if (minutos.isNumber()) return minutos.asInt();
        return jugador.path("stats").path("minutesPlayed").asInt();
    }

    private static String ratingFinal(JsonNode jugador) {
        JsonNode rating = jugador.path("rating");
        if (!rating.isMissingNode() && !rating.isNull()) return rating.asText();
        return jugador.path("stats").path("rating").asText();
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
