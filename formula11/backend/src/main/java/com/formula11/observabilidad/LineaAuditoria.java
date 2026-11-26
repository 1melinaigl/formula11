package com.formula11.observabilidad;

/**
 * Una línea del archivo de auditoría: quién llamó a qué operación, con qué parámetros,
 * cuánto tardó y cómo terminó. La fecha y el correlation ID los agrega el patrón de logback.
 */
public record LineaAuditoria(String usuario, String operacion, String parametros, long duracionMs, String resultado) {
    public static final String OK = "OK";
    private static final String SIN_PARAMETROS = "{}";

    public LineaAuditoria {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario de la auditoría es obligatorio");
        }
        if (duracionMs < 0) {
            throw new IllegalArgumentException("La duración no puede ser negativa");
        }
        if (parametros == null || parametros.isBlank()) {
            parametros = SIN_PARAMETROS;
        }
    }

    public static String resultadoError(Throwable error) {
        return "ERROR(" + error.getClass().getSimpleName() + ")";
    }

    public String formatear() {
        return "usuario=" + usuario
                + " operacion=" + operacion
                + " parametros=" + parametros
                + " duracionMs=" + duracionMs
                + " resultado=" + resultado;
    }
}
