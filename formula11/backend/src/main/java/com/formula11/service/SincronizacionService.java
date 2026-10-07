package com.formula11.service;

import com.formula11.adapter.ExternalPlayer;
import com.formula11.adapter.ExternalTeam;
import com.formula11.adapter.FootballDataException;
import com.formula11.adapter.FootballDataPort;
import com.formula11.dto.LigaSyncResultResponse;
import com.formula11.dto.SincronizacionResponse;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SincronizacionService {
    private static final String PROVIDER = "FOOTBALL_DATA";

    private final FootballDataPort footballDataPort;
    private final JugadorRepository jugadorRepository;

    public SincronizacionService(FootballDataPort footballDataPort, JugadorRepository jugadorRepository) {
        this.footballDataPort = footballDataPort;
        this.jugadorRepository = jugadorRepository;
    }

    @Transactional
    public SincronizacionResponse sincronizar() {
        if (!footballDataPort.estaConfigurado()) {
            throw new FootballDataException(FootballDataException.Tipo.NO_CONFIGURADO, "La integración de fútbol no está configurada");
        }
        List<LigaSyncResultResponse> resultados = new ArrayList<>();
        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        int exitosas = 0;
        jugadorRepository.deleteAllByOrigen("LEGACY");

        for (Liga liga : Liga.values()) {
            try {
                int[] totales = sincronizarLiga(liga);
                creados += totales[0];
                actualizados += totales[1];
                omitidos += totales[2];
                exitosas++;
                resultados.add(new LigaSyncResultResponse(liga.getNombreLegible(), "COMPLETADA", totales[0], totales[1], totales[2], null));
            } catch (FootballDataException exception) {
                resultados.add(new LigaSyncResultResponse(liga.getNombreLegible(), "FALLIDA", 0, 0, 0, exception.getMessage()));
            } catch (RuntimeException exception) {
                resultados.add(new LigaSyncResultResponse(liga.getNombreLegible(), "FALLIDA", 0, 0, 0, "No se pudo sincronizar la liga"));
            }
        }

        if (exitosas == 0) {
            throw new FootballDataException(FootballDataException.Tipo.PROVEEDOR, "No se pudo sincronizar ninguna liga");
        }
        String estado = exitosas == Liga.values().length ? "COMPLETADA" : "PARCIAL";
        return new SincronizacionResponse(estado, resultados, creados, actualizados, omitidos, MDC.get("correlationId"));
    }

    private int[] sincronizarLiga(Liga liga) {
        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        List<ExternalTeam> equipos = footballDataPort.obtenerEquipos(liga);
        for (ExternalTeam equipo : equipos) {
            for (ExternalPlayer player : footballDataPort.obtenerJugadores(equipo)) {
                Liga ligaJugador;
                try {
                    ligaJugador = Liga.valueOf(player.liga());
                } catch (IllegalArgumentException exception) {
                    omitidos++;
                    continue;
                }
                if (player.externalId() == null || player.externalId().isBlank()
                        || player.nombre() == null || player.equipo() == null || player.posicion() == null) {
                    omitidos++;
                    continue;
                }
                Jugador jugador = jugadorRepository.findByExternalProviderAndExternalId(PROVIDER, player.externalId()).orElse(null);
                if (jugador == null) {
                    jugadorRepository.save(new Jugador(player.nombre(), player.equipo(), ligaJugador, player.posicion(), PROVIDER, player.externalId()));
                    creados++;
                } else {
                    jugador.actualizarDesdeExterno(PROVIDER, player.externalId(), player.nombre(), player.equipo(), ligaJugador, player.posicion());
                    jugadorRepository.save(jugador);
                    actualizados++;
                }
            }
        }
        return new int[]{creados, actualizados, omitidos};
    }
}
