package com.formula11.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae los datos de una página de partido de WhoScored guardada desde el navegador.
 * Formato verificado en una página real (Ctrl+U):
 * <pre>
 * require.config.params["args"] = {
 *     matchId:1994169,
 *     matchCentreData: {...},
 *     matchCentreEventTypeJson: {...},
 *     ...
 * </pre>
 * Las claves no llevan comillas, así que se busca cada clave seguida de dos puntos.
 */
public final class WhoScoredHtmlParser {
    private static final String DATA_KEY = "matchCentreData";
    private static final String TYPES_KEY = "matchCentreEventTypeJson";
    private static final Pattern MATCH_ID = Pattern.compile("\\bmatchId\\s*:\\s*(\\d+)");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WhoScoredHtmlParser() {}

    public static DatosHtml extraer(String html) {
        Objects.requireNonNull(html, "El HTML es obligatorio");
        return new DatosHtml(extraerMatchId(html), extraerObjeto(html, DATA_KEY),
                extraerObjeto(html, TYPES_KEY));
    }

    private static long extraerMatchId(String html) {
        Matcher matcher = MATCH_ID.matcher(html);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : -1;
    }

    private static String extraerObjeto(String html, String key) {
        int inicio = inicioObjeto(html, key);
        return leerObjeto(html, key, inicio);
    }

    private static int inicioObjeto(String html, String key) {
        Matcher matcher = Pattern.compile("\\b" + key + "\\s*[:=]\\s*\\{").matcher(html);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No se encontró " + key);
        }
        return matcher.end() - 1;
    }

    private static String leerObjeto(String html, String key, int inicio) {
        EstadoLectura estado = new EstadoLectura();
        for (int i = inicio; i < html.length(); i++) {
            if (estado.avanzar(html.charAt(i))) {
                return validarObjeto(html.substring(inicio, i + 1), key);
            }
        }
        throw new IllegalArgumentException("Objeto incompleto para " + key);
    }

    private static String validarObjeto(String json, String key) {
        try {
            OBJECT_MAPPER.readTree(json);
            return json;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Objeto JSON inválido para " + key, exception);
        }
    }

    /** Sigue llaves y strings de JSON para saber dónde termina el objeto. */
    private static final class EstadoLectura {
        private int profundidad;
        private boolean dentroDeString;
        private boolean escape;

        /** Devuelve true cuando el carácter cierra el objeto de nivel superior. */
        boolean avanzar(char actual) {
            if (dentroDeString) {
                avanzarDentroDeString(actual);
                return false;
            }
            if (actual == '"') {
                dentroDeString = true;
            } else if (actual == '{') {
                profundidad++;
            } else if (actual == '}') {
                profundidad--;
                return profundidad == 0;
            }
            return false;
        }

        private void avanzarDentroDeString(char actual) {
            if (escape) {
                escape = false;
            } else if (actual == '\\') {
                escape = true;
            } else if (actual == '"') {
                dentroDeString = false;
            }
        }
    }

    public record DatosHtml(long matchId, String matchCentreData, String matchCentreEventTypeJson) {}
}
