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
import java.time.LocalDate;

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
        String id = String.valueOf(resumen.idExterno());
        if (partidoRepository.existsByFuenteAndIdExterno(FUENTE, id)) {
            return new CargaPartidoService.ResultadoPartido(false, 0, 0);
        }
        Instant ahora = Instant.now(clock);
        LocalDate fecha = resumen.startTime().toLocalDate();
        PartidoExterno externo = partidoRepository.save(new PartidoExterno(FUENTE, id, resumen.liga(),
                fecha, resumen.equipoLocal(), resumen.equipoVisitante(), ahora));
        int vinculados = 0;
        int pendientes = 0;
        for (MatchCentreParser.Jugador jugadorWhoScored : partido.jugadores()) {
            var vinculo = vinculacionService.vincular(jugadorWhoScored, resumen.liga());
            if (vinculo.vinculado()) {
                guardarMetrica(vinculo.jugador(), fecha, jugadorWhoScored, externo);
                vinculados++;
            } else {
                registrarPendiente(jugadorWhoScored, resumen, ahora);
                pendientes++;
            }
        }
        return new CargaPartidoService.ResultadoPartido(true, vinculados, pendientes);
    }

    private void guardarMetrica(Jugador jugador, LocalDate fecha, MatchCentreParser.Jugador datos,
                                PartidoExterno externo) {
        MetricaPartido metrica = new MetricaPartido(jugador, fecha, datos.minutos(), datos.titular(),
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
