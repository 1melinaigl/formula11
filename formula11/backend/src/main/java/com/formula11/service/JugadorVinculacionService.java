package com.formula11.service;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.whoscored.MatchCentreParser;
import com.formula11.model.whoscored.NormalizadorNombre;
import com.formula11.persistence.JugadorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class JugadorVinculacionService {
    private final JugadorRepository jugadorRepository;

    public JugadorVinculacionService(JugadorRepository jugadorRepository) {
        this.jugadorRepository = Objects.requireNonNull(jugadorRepository);
    }

    public ResultadoVinculacion vincular(MatchCentreParser.Jugador jugadorWhoScored, Liga liga) {
        String whoscoredId = String.valueOf(jugadorWhoScored.playerId());
        var existing = jugadorRepository.findByWhoscoredId(whoscoredId);
        if (existing.isPresent()) return new ResultadoVinculacion(existing.get(), false);

        String nombre = NormalizadorNombre.normalizar(jugadorWhoScored.nombre());
        List<Jugador> candidates = jugadorRepository.findByLiga(liga).stream()
                .filter(jugador -> nombre.equals(NormalizadorNombre.normalizar(jugador.getNombre())))
                .toList();
        if (candidates.size() > 1) {
            String equipo = NormalizadorNombre.normalizar(jugadorWhoScored.equipo());
            candidates = candidates.stream()
                    .filter(candidate -> contieneEquipo(equipo, NormalizadorNombre.normalizar(candidate.getEquipo())))
                    .toList();
        }
        if (candidates.size() != 1) return new ResultadoVinculacion(null, true);

        Jugador jugador = candidates.get(0);
        jugador.asignarWhoscoredId(whoscoredId);
        jugadorRepository.save(jugador);
        return new ResultadoVinculacion(jugador, false);
    }

    private static boolean contieneEquipo(String primero, String segundo) {
        return primero.contains(segundo) || segundo.contains(primero);
    }

    public record ResultadoVinculacion(Jugador jugador, boolean pendiente) {
        public boolean vinculado() {
            return jugador != null;
        }
    }
}
