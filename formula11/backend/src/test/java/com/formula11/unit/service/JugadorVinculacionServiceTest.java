package com.formula11.unit.service;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.whoscored.MatchCentreParser;
import com.formula11.persistence.JugadorRepository;
import com.formula11.service.JugadorVinculacionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JugadorVinculacionServiceTest {
    private final JugadorRepository repository = mock(JugadorRepository.class);
    private final JugadorVinculacionService service = new JugadorVinculacionService(repository);

    @Test
    void vinculaCandidatoUnicoYGuardaId() {
        Jugador jugador = jugador("Ángel Fortuño", "Espanyol FC");
        when(repository.findByLiga(Liga.LA_LIGA)).thenReturn(List.of(jugador));

        var resultado = service.vincular(who("Ángel Fortuño", "Espanyol", 449409), Liga.LA_LIGA);

        assertTrue(resultado.vinculado());
        assertSame(jugador, resultado.jugador());
        assertEquals("449409", jugador.getWhoscoredId());
        verify(repository).save(jugador);
    }

    @Test
    void resuelveEquipoContenido() {
        Jugador jugador = jugador("Juan Pérez", "Arsenal FC");
        when(repository.findByLiga(Liga.PREMIER_LEAGUE)).thenReturn(List.of(jugador));

        var resultado = service.vincular(who("Juan Pérez", "Arsenal", 12), Liga.PREMIER_LEAGUE);

        assertTrue(resultado.vinculado());
        assertSame(jugador, resultado.jugador());
    }

    @Test
    void dejaPendienteHomónimoAmbiguoSinModificarJugadores() {
        Jugador primero = jugador("Juan Pérez", "Arsenal FC");
        Jugador segundo = jugador("Juan Pérez", "Chelsea FC");
        when(repository.findByLiga(Liga.PREMIER_LEAGUE)).thenReturn(List.of(primero, segundo));

        var resultado = service.vincular(who("Juan Pérez", "Liverpool", 12), Liga.PREMIER_LEAGUE);

        assertFalse(resultado.vinculado());
        assertTrue(resultado.pendiente());
        assertNull(primero.getWhoscoredId());
        assertNull(segundo.getWhoscoredId());
        verify(repository, never()).save(any());
    }

    @Test
    void dejaPendienteCuandoNoHayCandidato() {
        when(repository.findByLiga(Liga.LA_LIGA)).thenReturn(List.of());

        var resultado = service.vincular(who("Jugador Ausente", "Málaga", 12), Liga.LA_LIGA);

        assertFalse(resultado.vinculado());
        assertTrue(resultado.pendiente());
        verify(repository, never()).save(any());
    }

    private static Jugador jugador(String nombre, String equipo) {
        return new Jugador(nombre, equipo, Liga.LA_LIGA, "MED");
    }

    private static MatchCentreParser.Jugador who(String nombre, String equipo, long id) {
        return new MatchCentreParser.Jugador(id, nombre, equipo, true, 90,
                0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0);
    }
}
