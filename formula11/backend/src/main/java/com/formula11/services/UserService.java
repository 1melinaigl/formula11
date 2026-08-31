package com.formula11.services;

import com.formula11.dto.CreateUserRequest;
import com.formula11.dto.UserResponse;
import com.formula11.exception.DuplicateResourceException;
import com.formula11.models.User;
import com.formula11.repositories.UserRepository;
import com.formula11.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${api.key.prefix:F11}")
    private String apiKeyPrefix;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String apiKey = apiKeyPrefix + "-" + UUID.randomUUID();
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .apiKey(apiKey)
                .build();

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getUsername());

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .apiKey(savedUser.getApiKey())
                .token(token)
                .build();
    }
}
