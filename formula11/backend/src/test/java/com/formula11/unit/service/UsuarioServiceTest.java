package com.formula11.unit.service;

import com.formula11.dto.RegistroUsuarioRequest;
import com.formula11.model.Usuario;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.security.JwtService;
import com.formula11.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void registrarCreaUsuarioYDevuelveTokenCuandoElEmailNoExiste() {
        var request = new RegistroUsuarioRequest("Ana", "ana@example.com", "Secreto123!");
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secreto123!")).thenReturn("hash-encriptado");
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario usuario = invocacion.getArgument(0);
            ReflectionTestUtils.setField(usuario, "id", 1L);
            return usuario;
        });
        when(jwtService.generarToken(anyLong(), eq("ana@example.com"))).thenReturn("token-jwt");

        var respuesta = usuarioService.registrar(request);

        assertEquals(1L, respuesta.id());
        assertEquals("Ana", respuesta.nombre());
        assertEquals("ana@example.com", respuesta.email());
        assertEquals("token-jwt", respuesta.token());
        verify(usuarioRepository).saveAndFlush(any(Usuario.class));
    }

    @Test
    void registrarRechazaEmailYaExistenteSinConsultarLaBase() {
        var request = new RegistroUsuarioRequest("Ana", "ana@example.com", "Secreto123!");
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(UsuarioService.EmailDuplicadoException.class, () -> usuarioService.registrar(request));

        verify(usuarioRepository, never()).saveAndFlush(any());
        verify(jwtService, never()).generarToken(anyLong(), any());
    }

    @Test
    void registrarConvierteViolacionDeIntegridadEnEmailDuplicado() {
        var request = new RegistroUsuarioRequest("Ana", "ana@example.com", "Secreto123!");
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secreto123!")).thenReturn("hash-encriptado");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("email duplicado"));

        assertThrows(UsuarioService.EmailDuplicadoException.class, () -> usuarioService.registrar(request));

        verify(jwtService, never()).generarToken(anyLong(), any());
    }
}