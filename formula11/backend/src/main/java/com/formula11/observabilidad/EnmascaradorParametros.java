package com.formula11.observabilidad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Convierte los argumentos de un endpoint en el texto {@code {nombre=valor, ...}} de la línea de
 * auditoría. Omite los argumentos de infraestructura, reemplaza por {@value #MASCARA} las
 * contraseñas y credenciales (a cualquier profundidad) y recorta el resultado para que una línea
 * no crezca sin límite.
 */
@Component
public class EnmascaradorParametros {
    public static final int LARGO_MAXIMO = 500;
    public static final String MARCA_RECORTE = "…(recortado)";
    public static final String MASCARA = "****";

    /** Nombres de campo (en minúsculas) que nunca se escriben: contraseñas y credenciales. */
    private static final Set<String> CAMPOS_SENSIBLES =
            Set.of("password", "contrasena", "token", "accesstoken", "refreshtoken", "secret");

    private final ObjectMapper objectMapper;

    public EnmascaradorParametros(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String enmascarar(String[] nombres, Object[] valores) {
        StringJoiner parametros = new StringJoiner(", ", "{", "}");
        for (int i = 0; i < valores.length; i++) {
            Object valor = valores[i];
            if (esInfraestructura(valor)) {
                continue;
            }
            String nombre = nombres != null && i < nombres.length ? nombres[i] : "arg" + i;
            parametros.add(nombre + "=" + (esSensible(nombre) ? MASCARA : mostrar(valor)));
        }
        return recortar(parametros.toString());
    }

    private static boolean esInfraestructura(Object valor) {
        return valor instanceof Principal
                || valor instanceof ServletRequest
                || valor instanceof ServletResponse
                || valor instanceof HttpSession
                || valor instanceof Errors;
    }

    private String mostrar(Object valor) {
        if (valor == null) {
            return "null";
        }
        JsonNode nodo;
        try {
            nodo = objectMapper.valueToTree(valor);
        } catch (IllegalArgumentException e) {
            return String.valueOf(valor);
        }
        if (nodo.isValueNode()) {
            return nodo.asText();
        }
        enmascararCampos(nodo);
        return nodo.toString();
    }

    private static void enmascararCampos(JsonNode nodo) {
        if (nodo instanceof ObjectNode objeto) {
            for (Map.Entry<String, JsonNode> campo : objeto.properties()) {
                if (esSensible(campo.getKey())) {
                    campo.setValue(TextNode.valueOf(MASCARA));
                } else {
                    enmascararCampos(campo.getValue());
                }
            }
        } else if (nodo.isArray()) {
            nodo.forEach(EnmascaradorParametros::enmascararCampos);
        }
    }

    private static boolean esSensible(String nombre) {
        return nombre != null && CAMPOS_SENSIBLES.contains(nombre.toLowerCase(Locale.ROOT));
    }

    private static String recortar(String texto) {
        if (texto.length() <= LARGO_MAXIMO) {
            return texto;
        }
        return texto.substring(0, LARGO_MAXIMO) + MARCA_RECORTE;
    }
}
