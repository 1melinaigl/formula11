package com.formula11.service;

import com.formula11.adapter.PartidoWhoScoredResumen;
import com.formula11.model.Jugador;
import com.formula11.model.MetricaPartido;
import com.formula11.model.PartidoExterno;
import com.formula11.model.PendienteVinculacion;
import com.formula11.model.whoscored.MatchCentreParser;
import com.formula11.persistence.MetricaPartidoRepository;
import com.formula11.persistence.PartidoExternoRepository;
import com.formula11.persistence.PendienteVinculacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Guarda un partido ya descargado y parseado en una única transacción: verificación de idempotencia,
 * partido_externo, métricas de los jugadores vinculados y pendientes. Está separado de
 * {@link CargaPartidoService} para que @Transactional se aplique (las llamadas internas vía this
 * no pasan por el proxy de Spring).
 */
@Service
public class GuardadoPartidoService {
    static final String FUENTE = "WHOSCORED";
    private final PartidoExternoRepository partidoRepository;
    private final MetricaPartidoRepository metricaRepository;
    private final PendienteVinculacionRepository pendienteRepository;
    private final JugadorVinculacionService vinculacionService;
    private final Clock clock;

    public GuardadoPartidoService(PartidoExternoRepository partidoRepository,
                                  MetricaPartidoRepository metricaRepository,
                                  PendienteVinculacionRepository pendienteRepository,
                                  JugadorVinculacionService vinculacionService, Clock clock) {
        this.partidoRepository = partidoRepository;
        this.metricaRepository = metricaRepository;
        this.pendienteRepository = pendienteRepository;
        this.vinculacionService = vinculacionService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean yaCargado(long idExterno) {
        return partidoRepository.existsByFuenteAndIdExterno(FUENTE, String.valueOf(idExterno));
    }

    @Transactional
    public CargaPartidoService.ResultadoPartido guardar(PartidoWhoScoredResumen resumen,
                                                        MatchCentreParser.Partido partido) {
        if (yaCargado(resumen.idExterno())) {
            return new CargaPartidoService.ResultadoPartido(false, 0, 0);
        }
        Instant ahora = Instant.now(clock);
        PartidoExterno externo = crearPartidoExterno(resumen, ahora);
        List<MatchCentreParser.Jugador> jugadores = partido.jugadores();
        int vinculados = 0;
        for (MatchCentreParser.Jugador jugador : jugadores) {
            if (guardarJugador(jugador, resumen, externo, ahora)) {
                vinculados++;
            }
        }
        return new CargaPartidoService.ResultadoPartido(true, vinculados, jugadores.size() - vinculados);
    }

    private PartidoExterno crearPartidoExterno(PartidoWhoScoredResumen resumen, Instant ahora) {
        return partidoRepository.save(new PartidoExterno(FUENTE, String.valueOf(resumen.idExterno()),
                resumen.liga(), resumen.startTime().toLocalDate(), resumen.equipoLocal(),
                resumen.equipoVisitante(), ahora));
    }

    /**
     * Guarda la métrica del jugador si se pudo vincular a uno del catálogo; si no, lo deja pendiente.
     * Devuelve si quedó vinculado.
     */
    private boolean guardarJugador(MatchCentreParser.Jugador datos, PartidoWhoScoredResumen resumen,
                                   PartidoExterno externo, Instant ahora) {
        var vinculo = vinculacionService.vincular(datos, resumen.liga());
        if (!vinculo.vinculado()) {
            registrarPendiente(datos, resumen, ahora);
            return false;
        }
        guardarMetrica(vinculo.jugador(), datos, externo);
        return true;
    }

    private void guardarMetrica(Jugador jugador, MatchCentreParser.Jugador datos, PartidoExterno externo) {
        MetricaPartido metrica = new MetricaPartido(jugador, externo.getFechaPartido(), datos.minutos(), datos.titular(),
                datos.goles(), datos.asistencias(), datos.tiros(), datos.pasesClave(), datos.regates(),
                datos.entradas(), datos.intercepciones(), datos.amarillas(), datos.rojas(), datos.atajadas(),
                datos.golesRecibidos(), datos.vallaInvicta(), datos.rating(), FUENTE);
        metrica.asociarPartidoExterno(externo);
        metricaRepository.save(metrica);
    }

    /** Un jugador sin vincular queda una sola vez como pendiente, aunque aparezca en muchos partidos. */
    private void registrarPendiente(MatchCentreParser.Jugador datos, PartidoWhoScoredResumen resumen,
                                    Instant ahora) {
        String whoscoredId = String.valueOf(datos.playerId());
        if (!pendienteRepository.existsByFuenteAndWhoscoredIdAndResueltoFalse(FUENTE, whoscoredId)) {
            pendienteRepository.save(new PendienteVinculacion(FUENTE, whoscoredId, datos.nombre(),
                    datos.equipo(), resumen.liga(), ahora));
        }
    }
}
