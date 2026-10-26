package com.formula11.service;

import com.formula11.config.MercadoProperties;
import com.formula11.dto.RegistroUsuarioRequest;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.JugadorRepository;
import com.formula11.persistence.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Profile("local")
@Order(2)
public class DemoPortfolioInitializer implements ApplicationRunner {
    private static final String PASSWORD = "DemoFormula11!";
    // Tokens que compra cada usuario de demo de cada jugador; distintos para que los portfolios se diferencien.
    private static final int[] CANTIDADES = {10, 8, 5, 3};
    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioService usuarioService;
    private final MercadoService mercadoService;
    private final MercadoProperties properties;

    public DemoPortfolioInitializer(UsuarioRepository usuarioRepository,
                                    JugadorRepository jugadorRepository,
                                    UsuarioService usuarioService,
                                    MercadoService mercadoService,
                                    MercadoProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioService = usuarioService;
        this.mercadoService = mercadoService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String> emails = properties.getDemoUsuarios();
        if (emails.size() != CANTIDADES.length) {
            throw new IllegalStateException("mercado.demo.usuarios debe contener exactamente cuatro emails");
        }
        String primerEmail = normalizar(emails.get(0));
        if (usuarioRepository.existsByEmail(primerEmail)) {
            return;
        }

        List<Jugador> jugadores = unoPorLiga(jugadorRepository.findAll());

        Clock clockDemo = Clock.fixed(
                properties.getDemoFechaInicio().atStartOfDay().toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC);
        for (int indiceUsuario = 0; indiceUsuario < emails.size(); indiceUsuario++) {
            String email = normalizar(emails.get(indiceUsuario));
            usuarioService.registrar(new RegistroUsuarioRequest(
                    "Demo " + (indiceUsuario + 1), email, PASSWORD));
            Long usuarioId = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se pudo recuperar el usuario demo " + email))
                    .getId();
            for (Jugador jugador : jugadores) {
                mercadoService.comprar(usuarioId, jugador.getId(), CANTIDADES[indiceUsuario], clockDemo);
            }
        }
    }

    // El primer jugador (menor id) de cada liga, en el orden de Liga: uno por liga también sirve
    // para mostrar en la demo la cotización de un jugador de cada liga.
    private static List<Jugador> unoPorLiga(List<Jugador> todos) {
        Map<Liga, Jugador> primeros = todos.stream().collect(Collectors.toMap(
                Jugador::getLiga, jugador -> jugador,
                (uno, otro) -> uno.getId() <= otro.getId() ? uno : otro));
        if (primeros.size() < Liga.values().length) {
            throw new RecursoNoEncontradoException(
                    "Se necesita al menos un jugador de cada liga para cargar la demo");
        }
        return Arrays.stream(Liga.values()).map(primeros::get).toList();
    }

    private static String normalizar(String email) {
        return com.formula11.model.Usuario.normalizarEmail(email);
    }
}
