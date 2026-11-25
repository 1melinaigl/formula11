package com.formula11.service.jugadores;

import com.formula11.adapter.footballdata.ExternalPlayer;
import com.formula11.adapter.footballdata.ExternalTeam;
import com.formula11.adapter.footballdata.FootballDataException;
import com.formula11.adapter.footballdata.FootballDataPort;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.jugadores.JugadorRepository;
import com.formula11.persistence.mercado.TenenciaRepository;
import com.formula11.persistence.mercado.TransaccionRepository;
import com.formula11.persistence.usuarios.UsuarioRepository;
import com.formula11.model.Rol;
import com.formula11.model.Tenencia;
import com.formula11.model.Usuario;
import com.formula11.config.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SincronizacionService {
    private static final String PROVIDER = "FOOTBALL_DATA";
    private static final int TOKENS_INICIALES = 100;

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
    public SincronizacionResultado sincronizar() {
        if (!footballDataPort.estaConfigurado()) {
            throw new FootballDataException(FootballDataException.Tipo.NO_CONFIGURADO, "La integración de fútbol no está configurada");
        }
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO).orElse(null);
        eliminarJugadoresLegacySinOperaciones(superusuario);

        List<SincronizacionResultado.LigaSincronizada> resultados = new ArrayList<>();
        Totales totales = Totales.VACIO;
        int exitosas = 0;
        for (Liga liga : Liga.values()) {
            try {
                Totales deLiga = sincronizarLiga(liga, superusuario);
                totales = totales.sumar(deLiga);
                exitosas++;
                resultados.add(ligaCompletada(liga, deLiga));
            } catch (FootballDataException exception) {
                resultados.add(ligaFallida(liga, exception.getMessage()));
            } catch (RuntimeException exception) {
                resultados.add(ligaFallida(liga, "No se pudo sincronizar la liga"));
            }
        }

        if (exitosas == 0) {
            throw new FootballDataException(FootballDataException.Tipo.PROVEEDOR, "No se pudo sincronizar ninguna liga");
        }
        String estado = exitosas == Liga.values().length ? "COMPLETADA" : "PARCIAL";
        return new SincronizacionResultado(estado, resultados, totales.creados(), totales.actualizados(),
                totales.omitidos(), MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    /** Los jugadores del seed inicial se reemplazan por los del proveedor, salvo que alguien ya haya operado con ellos. */
    private void eliminarJugadoresLegacySinOperaciones(Usuario superusuario) {
        for (Jugador jugador : jugadorRepository.findAllByOrigen("LEGACY")) {
            if (transaccionRepository.existsByJugadorId(jugador.getId())) {
                continue;
            }
            if (superusuario != null) {
                tenenciaRepository.deleteByUsuarioIdAndJugadorId(superusuario.getId(), jugador.getId());
            }
            jugadorRepository.delete(jugador);
        }
    }

    private Totales sincronizarLiga(Liga liga, Usuario superusuario) {
        Totales totales = Totales.VACIO;
        for (ExternalTeam equipo : footballDataPort.obtenerEquipos(liga)) {
            for (ExternalPlayer player : footballDataPort.obtenerJugadores(equipo)) {
                totales = totales.sumar(sincronizarJugador(player, superusuario));
            }
        }
        return totales;
    }

    private ResultadoJugador sincronizarJugador(ExternalPlayer player, Usuario superusuario) {
        if (!esValidoParaProcesar(player)) {
            return ResultadoJugador.OMITIDO;
        }
        Liga liga = Liga.valueOf(player.liga());
        Optional<Jugador> existente = jugadorRepository.findByExternalProviderAndExternalId(PROVIDER, player.externalId());
        if (existente.isPresent()) {
            actualizarJugador(existente.get(), player, liga);
            return ResultadoJugador.ACTUALIZADO;
        }
        crearJugador(player, liga, superusuario);
        return ResultadoJugador.CREADO;
    }

    /** Un jugador nuevo arranca con sus 100 tokens en manos del superusuario. */
    private void crearJugador(ExternalPlayer player, Liga liga, Usuario superusuario) {
        Jugador guardado = jugadorRepository.save(new Jugador(player.nombre(), player.equipo(), liga,
                player.posicion(), PROVIDER, player.externalId()));
        if (superusuario != null) {
            tenenciaRepository.save(new Tenencia(superusuario, guardado, TOKENS_INICIALES, BigDecimal.ONE));
        }
    }

    private void actualizarJugador(Jugador jugador, ExternalPlayer player, Liga liga) {
        jugador.actualizarDesdeExterno(PROVIDER, player.externalId(), player.nombre(), player.equipo(), liga,
                player.posicion());
        jugadorRepository.save(jugador);
    }

    private static SincronizacionResultado.LigaSincronizada ligaCompletada(Liga liga, Totales totales) {
        return new SincronizacionResultado.LigaSincronizada(liga.getNombreLegible(), "COMPLETADA", totales.creados(),
                totales.actualizados(), totales.omitidos(), null);
    }

    private static SincronizacionResultado.LigaSincronizada ligaFallida(Liga liga, String error) {
        return new SincronizacionResultado.LigaSincronizada(liga.getNombreLegible(), "FALLIDA", 0, 0, 0, error);
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

    private enum ResultadoJugador { CREADO, ACTUALIZADO, OMITIDO }

    private record Totales(int creados, int actualizados, int omitidos) {
        static final Totales VACIO = new Totales(0, 0, 0);

        Totales sumar(ResultadoJugador resultado) {
            return switch (resultado) {
                case CREADO -> new Totales(creados + 1, actualizados, omitidos);
                case ACTUALIZADO -> new Totales(creados, actualizados + 1, omitidos);
                case OMITIDO -> new Totales(creados, actualizados, omitidos + 1);
            };
        }

        Totales sumar(Totales otros) {
            return new Totales(creados + otros.creados, actualizados + otros.actualizados, omitidos + otros.omitidos);
        }
    }
}
