package com.formula11.service;

import com.formula11.config.MercadoProperties;
import com.formula11.model.*;
import com.formula11.persistence.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;

@Component
public class SuperusuarioInitializer implements ApplicationRunner {
    private static final int TOKENS_EMITIDOS = 100;
    private static final BigDecimal PRECIO_INICIAL = BigDecimal.ONE;
    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final TenenciaRepository tenenciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final MercadoProperties properties;
    private final Clock clock;

    public SuperusuarioInitializer(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                                   TenenciaRepository tenenciaRepository, PasswordEncoder passwordEncoder,
                                   MercadoProperties properties, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.tenenciaRepository = tenenciaRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO).orElseGet(() -> {
            Usuario nuevo = new Usuario("Superusuario",
                    properties.getSuperusuarioEmail(),
                    passwordEncoder.encode(properties.getSuperusuarioPassword()),
                    clock.instant());
            nuevo.asignarRol(Rol.SUPERUSUARIO);
            return usuarioRepository.saveAndFlush(nuevo);
        });
        for (Jugador jugador : jugadorRepository.findAll()) {
            if (tenenciaRepository.findByUsuarioIdAndJugadorId(superusuario.getId(), jugador.getId()).isEmpty()) {
                tenenciaRepository.save(new Tenencia(superusuario, jugador, TOKENS_EMITIDOS, PRECIO_INICIAL));
            }
        }
    }
}
