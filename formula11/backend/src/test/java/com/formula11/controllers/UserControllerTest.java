package com.formula11.controllers;

import com.formula11.dto.CreateUserRequest;
import com.formula11.dto.UserResponse;
import com.formula11.services.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserControllerTest {

    @Test
    void shouldRegisterUser() {
        UserService userService = Mockito.mock(UserService.class);
        UserController controller = new UserController(userService);

        CreateUserRequest request = CreateUserRequest.builder()
                .username("pepe")
                .email("pepe@example.com")
                .password("Password123")
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("pepe")
                .email("pepe@example.com")
                .apiKey("F11-abc")
                .token("jwt-token")
                .build();

        Mockito.when(userService.register(Mockito.any(CreateUserRequest.class))).thenReturn(response);

        ResponseEntity<UserResponse> result = controller.register(request);

        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("pepe", result.getBody().getUsername());
        assertEquals("F11-abc", result.getBody().getApiKey());
    }
}
