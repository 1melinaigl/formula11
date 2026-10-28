package com.formula11.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public final class WhoScoredHtmlParser {
    private static final String DATA_MARKER = "require.config.params.args.matchCentreData";
    private static final String TYPES_MARKER = "require.config.params.args.matchCentreEventTypeJson";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WhoScoredHtmlParser() {}

    public static DatosHtml extraer(String html) {
        Objects.requireNonNull(html, "El HTML es obligatorio");
        return new DatosHtml(extraerObjeto(html, DATA_MARKER),
                extraerObjeto(html, TYPES_MARKER));
    }

    private static String extraerObjeto(String html, String marker) {
        int inicio = inicioObjeto(html, marker);
        return leerObjeto(html, marker, inicio);
    }

    private static int inicioObjeto(String html, String marker) {
        int markerIndex = html.indexOf(marker);
        if (markerIndex < 0) {
            throw new IllegalArgumentException("No se encontró " + marker);
        }
        int inicio = html.indexOf('{', markerIndex + marker.length());
        if (inicio < 0) {
            throw new IllegalArgumentException("No se encontró objeto para " + marker);
        }
        return inicio;
    }

    private static String leerObjeto(String html, String marker, int inicio) {
        int profundidad = 0;
        boolean dentroDeString = false;
        boolean escape = false;
        for (int i = inicio; i < html.length(); i++) {
            char current = html.charAt(i);
            if (dentroDeString) {
                if (escape) {
                    escape = false;
                } else if (current == '\\') {
                    escape = true;
                } else if (current == '"') {
                    dentroDeString = false;
                }
            } else if (current == '"') {
                dentroDeString = true;
            } else if (current == '{') {
                profundidad++;
            } else if (current == '}' && --profundidad == 0) {
                return validarObjeto(html.substring(inicio, i + 1), marker);
            }
        }
        throw new IllegalArgumentException("Objeto incompleto para " + marker);
    }

    private static String validarObjeto(String json, String marker) {
        try {
            OBJECT_MAPPER.readTree(json);
            return json;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Objeto JSON inválido para " + marker, exception);
        }
    }

    public record DatosHtml(String matchCentreData, String matchCentreEventTypeJson) {}
}
