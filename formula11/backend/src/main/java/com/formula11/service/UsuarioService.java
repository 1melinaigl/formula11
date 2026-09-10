package com.formula11.service;

import com.formula11.dto.RegistroUsuarioRequest;
import com.formula11.dto.RegistroUsuarioResponse;
import com.formula11.model.Usuario;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegistroUsuarioResponse registrar(RegistroUsuarioRequest request) {
        String email = Usuario.normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) throw new EmailDuplicadoException();
        Usuario usuario = new Usuario(request.nombre().trim(), email, passwordEncoder.encode(request.password()));
        try {
            Usuario guardado = usuarioRepository.saveAndFlush(usuario);
            return RegistroUsuarioResponse.from(guardado, jwtService.generarToken(guardado.getId(), guardado.getEmail()));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailDuplicadoException();
        }
    }

    public static class EmailDuplicadoException extends RuntimeException {
        public EmailDuplicadoException() { super("El email ya está registrado"); }
    }
}
