package com.formula11.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

// lee saldoUsuario del snapshot despues de una auditoría de mercado
public final class AuditoriaSaldoParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuditoriaSaldoParser() {
    }

    public static BigDecimal parsear(String snapshot) {
        if (snapshot == null) {
            throw new IllegalStateException("La auditoría de saldo está incompleta");
        }
        JsonNode saldo;
        try {
            saldo = MAPPER.readTree(snapshot).path("saldoUsuario");
        } catch (Exception exception) {
            throw new IllegalStateException("La auditoría de saldo no es válida", exception);
        }
        if (!saldo.isNumber() && !saldo.isTextual()) {
            throw new IllegalStateException("La auditoría no contiene saldoUsuario");
        }
        return new BigDecimal(saldo.asText());
    }
}
