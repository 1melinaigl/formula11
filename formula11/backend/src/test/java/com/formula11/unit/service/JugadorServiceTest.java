package com.formula11.unit.service;

import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import com.formula11.service.JugadorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Test
    void listarPaginaMapeaContenidoYMetadatos() {
        Jugador zidane = new Jugador("Zidane", "Real Madrid", Liga.LA_LIGA, "Mediocampista");
        when(jugadorRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(zidane), PageRequest.of(1, 2, Sort.by("nombre")), 3));

        var resultado = jugadorService.listar(1, 2, "nombre,asc", "La Liga", "Madrid", "medio");

        assertEquals(List.of("Zidane"), resultado.content().stream().map(com.formula11.dto.JugadorResponse::nombre).toList());
        assertEquals(1, resultado.page());
        assertEquals(2, resultado.size());
        assertEquals(3, resultado.totalElements());
        assertEquals(2, resultado.totalPages());
        verify(jugadorRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void listarPaginaRechazaParametrosInvalidos() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> jugadorService.listar(-1, 20, null, null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> jugadorService.listar(0, 101, null, null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> jugadorService.listar(0, 20, "nombre,up", null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> jugadorService.listar(0, 20, null, "Liga inventada", null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> jugadorService.listar(0, 20, null, null, "   ", null)));
        verifyNoInteractions(jugadorRepository);
    }
}