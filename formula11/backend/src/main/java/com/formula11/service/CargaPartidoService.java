package com.formula11.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.DatosPartidoWhoScored;
import com.formula11.adapter.PartidoWhoScoredResumen;
import com.formula11.model.whoscored.MatchCentreParser;
import org.springframework.stereotype.Service;

/**
 * Parsea un partido fuera de cualquier transacción y delega el guardado transaccional en
 * {@link GuardadoPartidoService}, que es otro bean (así @Transactional se aplica).
 */
@Service
public class CargaPartidoService {
    private final GuardadoPartidoService guardadoPartidoService;
    private final MatchCentreParser parser;

    public CargaPartidoService(GuardadoPartidoService guardadoPartidoService, ObjectMapper objectMapper) {
        this.guardadoPartidoService = guardadoPartidoService;
        this.parser = new MatchCentreParser(objectMapper);
    }

    public boolean yaCargado(long idExterno) {
        return guardadoPartidoService.yaCargado(idExterno);
    }

    public ResultadoPartido prepararYGuardar(PartidoWhoScoredResumen resumen, DatosPartidoWhoScored datos) {
        MatchCentreParser.Partido partido;
        try {
            partido = parser.parse(datos.matchCentreData(), datos.matchCentreEventTypeJson());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("No se pudo parsear el partido " + resumen.idExterno(), exception);
        }
        return guardadoPartidoService.guardar(resumen, partido);
    }

    public record ResultadoPartido(boolean cargado, int vinculados, int pendientes) {}
}
