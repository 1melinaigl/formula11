package com.formula11.model.whoscored;

import java.text.Normalizer;
import java.util.Locale;

public final class NormalizadorNombre {
    private NormalizadorNombre() {}

    public static String normalizar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        return Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
