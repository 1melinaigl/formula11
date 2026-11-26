package com.formula11.observabilidad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.security.Principal;
import java.util.StringJoiner;

/**
 * Convierte los argumentos de un endpoint en el texto {@code {nombre=valor, ...}} de la línea de
 * auditoría. Omite los argumentos de infraestructura y recorta el resultado para que una línea
 * no crezca sin límite.
 */
@Component
public class EnmascaradorParametros {
    public static final int LARGO_MAXIMO = 500;
    public static final String MARCA_RECORTE = "…(recortado)";

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
            parametros.add(nombre + "=" + mostrar(valor));
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
        return nodo.isValueNode() ? nodo.asText() : nodo.toString();
    }

    private static String recortar(String texto) {
        if (texto.length() <= LARGO_MAXIMO) {
            return texto;
        }
        return texto.substring(0, LARGO_MAXIMO) + MARCA_RECORTE;
    }
}
