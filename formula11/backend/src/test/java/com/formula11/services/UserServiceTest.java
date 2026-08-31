package com.formula11.services;

import com.formula11.dto.CreateUserRequest;
import com.formula11.dto.UserResponse;
import com.formula11.models.User;
import com.formula11.repositories.UserRepository;
import com.formula11.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUserAndGenerateToken() {
        UserService service = new UserService(userRepository, passwordEncoder, jwtService);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("messi")
                .email("messi@example.com")
                .password("Password123")
                .build();

        when(userRepository.existsByUsername("messi")).thenReturn(false);
        when(userRepository.existsByEmail("messi@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setApiKey("F11-123");
            return user;
        });
        when(jwtService.generateToken("messi")).thenReturn("jwt-token");

        UserResponse response = service.register(request);

        assertNotNull(response);
        assertEquals("messi", response.getUsername());
        assertEquals("messi@example.com", response.getEmail());
        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getApiKey());
    }
}
