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
import com.formula11.persistence.TenenciaRepository;
import com.formula11.persistence.TransaccionRepository;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.model.Rol;
import com.formula11.model.Usuario;
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
    private final TenenciaRepository tenenciaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    public SincronizacionService(FootballDataPort footballDataPort, JugadorRepository jugadorRepository,
                                 TenenciaRepository tenenciaRepository, TransaccionRepository transaccionRepository,
                                 UsuarioRepository usuarioRepository) {
        this.footballDataPort = footballDataPort;
        this.jugadorRepository = jugadorRepository;
        this.tenenciaRepository = tenenciaRepository;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
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
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO).orElse(null);
        for (Jugador jugador : jugadorRepository.findAllByOrigen("LEGACY")) {
            if (!transaccionRepository.existsByJugadorId(jugador.getId())) {
                if (superusuario != null) {
                    tenenciaRepository.deleteByUsuarioIdAndJugadorId(superusuario.getId(), jugador.getId());
                }
                jugadorRepository.delete(jugador);
            }
        }

        for (Liga liga : Liga.values()) {
            try {
                int[] totales = sincronizarLiga(liga, superusuario);
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

    private int[] sincronizarLiga(Liga liga, Usuario superusuario) {
        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        List<ExternalTeam> equipos = footballDataPort.obtenerEquipos(liga);
        for (ExternalTeam equipo : equipos) {
            for (ExternalPlayer player : footballDataPort.obtenerJugadores(equipo)) {
                if (!esValidoParaProcesar(player)) {
                    omitidos++;
                    continue;
                }
                Liga ligaJugador = Liga.valueOf(player.liga());
                Jugador jugador = jugadorRepository.findByExternalProviderAndExternalId(PROVIDER, player.externalId()).orElse(null);
                if (jugador == null) {
                    Jugador guardado = jugadorRepository.save(new Jugador(player.nombre(), player.equipo(), ligaJugador, player.posicion(), PROVIDER, player.externalId()));
                    if (superusuario != null) {
                        tenenciaRepository.save(new com.formula11.model.Tenencia(superusuario, guardado, 100, java.math.BigDecimal.ONE));
                    }
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

    private boolean esValidoParaProcesar(ExternalPlayer player) {
        if (player.externalId() == null || player.externalId().isBlank()
                || player.nombre() == null || player.equipo() == null || player.posicion() == null) {
            return false;
        }
        try {
            Liga.valueOf(player.liga());
            return true;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }
}
