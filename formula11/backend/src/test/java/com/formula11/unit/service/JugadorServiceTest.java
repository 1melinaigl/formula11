package com.formula11.unit.service;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import com.formula11.service.JugadorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JugadorServiceTest {

    @Mock
    private JugadorRepository jugadorRepository;

    @InjectMocks
    private JugadorService jugadorService;

    @Test
    void listarDevuelveJugadoresMapeadosDesdeElRepositorio() {
        Jugador messi = new Jugador("Messi", "Inter Miami", Liga.LA_LIGA, "Delantero");
        when(jugadorRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(messi));

        var resultado = jugadorService.listar();

        assertEquals(1, resultado.size());
        assertEquals("Messi", resultado.get(0).nombre());
        assertEquals("La Liga", resultado.get(0).liga());
        verify(jugadorRepository).findAllByOrderByNombreAsc();
    }

    @Test
    void listarDevuelveListaVaciaSinTocarNadaMas() {
        when(jugadorRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        var resultado = jugadorService.listar();

        assertTrue(resultado.isEmpty());
        verify(jugadorRepository, times(1)).findAllByOrderByNombreAsc();
        verifyNoMoreInteractions(jugadorRepository);
    }
}