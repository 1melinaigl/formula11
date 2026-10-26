package com.formula11.unit.service;

import com.formula11.config.MercadoProperties;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.model.Usuario;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.service.DemoPortfolioInitializer;
import com.formula11.service.MercadoService;
import com.formula11.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoPortfolioInitializerTest {
    @Mock UsuarioRepository usuarios;
    @Mock JugadorRepository jugadores;
    @Mock UsuarioService usuarioService;
    @Mock MercadoService mercadoService;
    @Mock ApplicationArguments arguments;

    private MercadoProperties properties;
    private DemoPortfolioInitializer initializer;
    private List<String> emails;

    @BeforeEach
    void preparar() {
        emails = List.of(
                "demo1@formula11.com",
                "demo2@formula11.com",
                "demo3@formula11.com",
                "demo4@formula11.com");
        properties = new MercadoProperties();
        properties.setDemoUsuarios(emails);
        properties.setDemoFechaInicio(LocalDate.of(2026, 8, 1));
        initializer = new DemoPortfolioInitializer(
                usuarios, jugadores, usuarioService, mercadoService, properties);
    }

    @Test
    void primerArranqueCompraUnJugadorPorLigaConCantidadesDistintasPorUsuario() {
        when(usuarios.existsByEmail("demo1@formula11.com")).thenReturn(false);
        // Un jugador por liga (ids 1 a 5) y otro de La Liga con id mayor, que no debe elegirse.
        List<Jugador> jugadoresDemo = new java.util.ArrayList<>();
        Liga[] ligas = Liga.values();
        for (int indice = 0; indice < ligas.length; indice++) {
            jugadoresDemo.add(jugador(indice + 1L, ligas[indice]));
        }
        jugadoresDemo.add(jugador(99L, Liga.LA_LIGA));
        when(jugadores.findAll()).thenReturn(jugadoresDemo);
        when(usuarios.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            Usuario usuario = new Usuario("Demo", email, "hash");
            ReflectionTestUtils.setField(usuario, "id", 100L + emails.indexOf(email));
            return Optional.of(usuario);
        });

        initializer.run(arguments);

        verify(usuarioService, times(4)).registrar(any());
        int[] cantidades = {10, 8, 5, 3};
        for (int usuario = 0; usuario < cantidades.length; usuario++) {
            for (long jugadorId = 1; jugadorId <= 5; jugadorId++) {
                verify(mercadoService).comprar(eq(100L + usuario), eq(jugadorId),
                        eq(cantidades[usuario]), any(java.time.Clock.class));
            }
        }
        verify(mercadoService, never()).comprar(anyLong(), eq(99L), anyInt(), any(java.time.Clock.class));
    }

    @Test
    void exigeUnJugadorDeCadaLigaParaCargarLaDemo() {
        when(usuarios.existsByEmail("demo1@formula11.com")).thenReturn(false);
        when(jugadores.findAll()).thenReturn(List.of(jugador(1L, Liga.LA_LIGA), jugador(2L, Liga.SERIE_A)));

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, () -> initializer.run(arguments));
        verifyNoInteractions(usuarioService, mercadoService);
    }

    private static Jugador jugador(long id, Liga liga) {
        Jugador jugador = new Jugador("Jugador " + id, "Equipo", liga, "DEL");
        ReflectionTestUtils.setField(jugador, "id", id);
        return jugador;
    }

    @Test
    void siDemo1ExisteNoDuplicaUsuariosNiOperaciones() {
        when(usuarios.existsByEmail("demo1@formula11.com")).thenReturn(true);

        initializer.run(arguments);

        verifyNoInteractions(usuarioService, mercadoService, jugadores);
    }

    @Test
    void exigeCincoJugadoresParaCargarLaDemo() {
        when(usuarios.existsByEmail("demo1@formula11.com")).thenReturn(false);
        when(jugadores.findAll()).thenReturn(List.of());

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class, () -> initializer.run(arguments));
        verifyNoInteractions(usuarioService, mercadoService);
    }

    @Test
    void soloSeRegistraEnPerfilLocal() {
        assertEquals("local",
                DemoPortfolioInitializer.class.getAnnotation(Profile.class).value()[0]);
    }
}
