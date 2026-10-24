package com.formula11.unit.service;

import com.formula11.adapter.ExternalPlayer;
import com.formula11.adapter.ExternalTeam;
import com.formula11.adapter.FootballDataPort;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Rol;
import com.formula11.model.Usuario;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.TenenciaRepository;
import com.formula11.persistence.TransaccionRepository;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.service.SincronizacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SincronizacionServiceTest {
    @Mock
    private FootballDataPort footballDataPort;
    @Mock
    private JugadorRepository jugadorRepository;
    @Mock
    private TenenciaRepository tenenciaRepository;
    @Mock
    private TransaccionRepository transaccionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private SincronizacionService sincronizacionService;

    @Test
    void sincronizaLasCincoLigasSinDuplicar() {
        when(footballDataPort.estaConfigurado()).thenReturn(true);
        when(footballDataPort.obtenerEquipos(any(Liga.class))).thenAnswer(invocation -> {
            Liga liga = invocation.getArgument(0);
            return List.of(new ExternalTeam("team-" + liga.name(), "Equipo " + liga.name(), liga.name()));
        });
        when(footballDataPort.obtenerJugadores(any(ExternalTeam.class))).thenAnswer(invocation -> {
            ExternalTeam equipo = invocation.getArgument(0);
            return List.of(new ExternalPlayer("player-" + equipo.externalId(), "Jugador", equipo.nombre(), equipo.liga(), "DEL"));
        });
        when(jugadorRepository.findByExternalProviderAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());

        var resultado = sincronizacionService.sincronizar();

        assertEquals("COMPLETADA", resultado.estado());
        assertEquals(5, resultado.ligas().size());
        assertEquals(5, resultado.creados());
        verify(jugadorRepository, times(5)).save(any());
    }

    @Test
    void creaTenenciaDeCienTokensDelSuperusuarioPorCadaJugadorNuevo() {
        Usuario superusuario = new Usuario("Super", "super@example.com", "hash");
        superusuario.asignarRol(Rol.SUPERUSUARIO);
        when(usuarioRepository.findByRol(Rol.SUPERUSUARIO)).thenReturn(Optional.of(superusuario));
        when(footballDataPort.estaConfigurado()).thenReturn(true);
        when(footballDataPort.obtenerEquipos(any(Liga.class))).thenAnswer(invocation -> {
            Liga liga = invocation.getArgument(0);
            return List.of(new ExternalTeam("team-" + liga.name(), "Equipo " + liga.name(), liga.name()));
        });
        when(footballDataPort.obtenerJugadores(any(ExternalTeam.class))).thenAnswer(invocation -> {
            ExternalTeam equipo = invocation.getArgument(0);
            return List.of(new ExternalPlayer("player-" + equipo.externalId(), "Jugador", equipo.nombre(), equipo.liga(), "DEL"));
        });
        when(jugadorRepository.findByExternalProviderAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());
        when(jugadorRepository.save(any(Jugador.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sincronizacionService.sincronizar();

        verify(tenenciaRepository, times(5)).save(argThat(tenencia ->
                tenencia.getUsuario() == superusuario && tenencia.getCantidad() == 100));
    }

    @Test
    void sinApikeyNoBorraLegacyNiLlamaAlRepositorio() {
        when(footballDataPort.estaConfigurado()).thenReturn(false);

        assertThrows(RuntimeException.class, () -> sincronizacionService.sincronizar());

        verifyNoInteractions(jugadorRepository);
        verify(footballDataPort, never()).obtenerEquipos(any());
    }

    @Test
    void omiteJugadoresInvalidosYActualizaLosExistentes() {
        when(footballDataPort.estaConfigurado()).thenReturn(true);
        ExternalTeam equipo = new ExternalTeam("team-1", "Equipo", Liga.LA_LIGA.name());
        when(footballDataPort.obtenerEquipos(any(Liga.class))).thenReturn(List.of(equipo));
        when(footballDataPort.obtenerJugadores(equipo)).thenReturn(List.of(
                new ExternalPlayer(null, "Sin ID", "Equipo", Liga.LA_LIGA.name(), "DEL"),
                new ExternalPlayer("bad-league", "Liga inválida", "Equipo", "INVALIDA", "DEL"),
                new ExternalPlayer("player-1", "Jugador actualizado", "Equipo", Liga.LA_LIGA.name(), "MED")));
        Jugador existente = new Jugador("Anterior", "Equipo", Liga.LA_LIGA, "DEL", "FOOTBALL_DATA", "player-1");
        when(jugadorRepository.findByExternalProviderAndExternalId("FOOTBALL_DATA", "player-1"))
                .thenReturn(Optional.of(existente));

        var resultado = sincronizacionService.sincronizar();

        assertEquals("COMPLETADA", resultado.estado());
        assertEquals(0, resultado.creados());
        assertEquals(5, resultado.actualizados());
        assertEquals(10, resultado.omitidos());
        verify(jugadorRepository, times(5)).save(existente);
    }

    @Test
    void devuelveEstadoParcialCuandoFallaUnaLiga() {
        when(footballDataPort.estaConfigurado()).thenReturn(true);
        when(footballDataPort.obtenerEquipos(Liga.PREMIER_LEAGUE))
                .thenThrow(new RuntimeException("proveedor caído"));
        when(footballDataPort.obtenerEquipos(argThat(liga -> liga != Liga.PREMIER_LEAGUE)))
                .thenReturn(List.of());

        var resultado = sincronizacionService.sincronizar();

        assertEquals("PARCIAL", resultado.estado());
        assertEquals(5, resultado.ligas().size());
        assertEquals("FALLIDA", resultado.ligas().get(0).estado());
        assertEquals("COMPLETADA", resultado.ligas().get(1).estado());
    }

    @Test
    void fallaSiNingunaLigaPuedeSincronizarse() {
        when(footballDataPort.estaConfigurado()).thenReturn(true);
        when(footballDataPort.obtenerEquipos(any(Liga.class)))
                .thenThrow(new RuntimeException("proveedor caído"));

        var exception = assertThrows(RuntimeException.class, () -> sincronizacionService.sincronizar());

        assertEquals("No se pudo sincronizar ninguna liga", exception.getMessage());
    }
}
