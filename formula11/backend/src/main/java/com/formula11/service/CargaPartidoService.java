package com.formula11.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formula11.adapter.DatosPartidoWhoScored;
import com.formula11.adapter.PartidoWhoScoredResumen;
import com.formula11.model.*;
import com.formula11.model.whoscored.MatchCentreParser;
import com.formula11.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class CargaPartidoService {
    private static final String FUENTE = "WHOSCORED";
    private final PartidoExternoRepository partidoRepository;
    private final MetricaPartidoRepository metricaRepository;
    private final PendienteVinculacionRepository pendienteRepository;
    private final JugadorVinculacionService vinculacionService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public CargaPartidoService(PartidoExternoRepository partidoRepository,
                               MetricaPartidoRepository metricaRepository,
                               PendienteVinculacionRepository pendienteRepository,
                               JugadorVinculacionService vinculacionService,
                               Clock clock, ObjectMapper objectMapper) {
        this.partidoRepository = partidoRepository;
        this.metricaRepository = metricaRepository;
        this.pendienteRepository = pendienteRepository;
        this.vinculacionService = vinculacionService;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public ResultadoPartido prepararYGuardar(PartidoWhoScoredResumen resumen, DatosPartidoWhoScored datos) {
        try {
            MatchCentreParser parser = new MatchCentreParser(objectMapper);
            MatchCentreParser.Partido partido = parser.parse(datos.matchCentreData(),
                    datos.matchCentreEventTypeJson());
            return guardar(resumen, partido);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("No se pudo cargar el partido " + resumen.idExterno(), exception);
        }
    }

    @Transactional
    protected ResultadoPartido guardar(PartidoWhoScoredResumen resumen, MatchCentreParser.Partido partido) {
        String id = String.valueOf(resumen.idExterno());
        if (partidoRepository.existsByFuenteAndIdExterno(FUENTE, id)) {
            return new ResultadoPartido(false, 0, 0);
        }
        PartidoExterno externo = partidoRepository.save(new PartidoExterno(FUENTE, id, resumen.liga(),
                resumen.startTime().toLocalDate(), resumen.equipoLocal(), resumen.equipoVisitante(),
                Instant.now(clock)));
        int vinculados = 0;
        int pendientes = 0;
        for (MatchCentreParser.Jugador jugadorWhoScored : partido.jugadores()) {
            var vinculo = vinculacionService.vincular(jugadorWhoScored, resumen.liga());
            if (!vinculo.vinculado()) {
                pendienteRepository.save(new PendienteVinculacion(FUENTE,
                        String.valueOf(jugadorWhoScored.playerId()), jugadorWhoScored.nombre(),
                        jugadorWhoScored.equipo(), resumen.liga(), Instant.now(clock)));
                pendientes++;
                continue;
            }
            Jugador jugador = vinculo.jugador();
            MetricaPartido metrica = new MetricaPartido(jugador, resumen.startTime().toLocalDate(),
                    jugadorWhoScored.minutos(), jugadorWhoScored.titular(), jugadorWhoScored.goles(),
                    jugadorWhoScored.asistencias(), jugadorWhoScored.tiros(), jugadorWhoScored.pasesClave(),
                    jugadorWhoScored.regates(), jugadorWhoScored.entradas(), jugadorWhoScored.intercepciones(),
                    jugadorWhoScored.amarillas(), jugadorWhoScored.rojas(), jugadorWhoScored.atajadas(),
                    jugadorWhoScored.golesRecibidos(), jugadorWhoScored.vallaInvicta(), jugadorWhoScored.rating(),
                    FUENTE);
            metrica.asociarPartidoExterno(externo);
            metricaRepository.save(metrica);
            vinculados++;
        }
        return new ResultadoPartido(true, vinculados, pendientes);
    }

    public record ResultadoPartido(boolean cargado, int vinculados, int pendientes) {}
}
