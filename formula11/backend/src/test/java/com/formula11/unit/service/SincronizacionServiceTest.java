package com.formula11.unit.service;

import com.formula11.adapter.ExternalPlayer;
import com.formula11.adapter.ExternalTeam;
import com.formula11.adapter.FootballDataPort;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
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
        verify(jugadorRepository).deleteAllByOrigen("LEGACY");
        verify(jugadorRepository, times(5)).save(any());
    }

    @Test
    void sinApikeyNoBorraLegacyNiLlamaAlRepositorio() {
        when(footballDataPort.estaConfigurado()).thenReturn(false);

        assertThrows(RuntimeException.class, () -> sincronizacionService.sincronizar());

        verifyNoInteractions(jugadorRepository);
        verify(footballDataPort, never()).obtenerEquipos(any());
    }
}
