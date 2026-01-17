package com.example.security;

import com.security.controller.AuthenticationController;
import com.security.dto.RegisterRequest;
import com.security.dto.UserDTO;
import com.security.reqeuest.AuthenticationRequest;
import com.security.response.AuthenticationResponse;
import com.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister() throws IOException {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@email.com");
        request.setFullName("Test User");
        request.setPassword("123456");

        UserDTO userDTO = UserDTO.builder().email("test@email.com").fullName("Test User").build();
        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken("jwt-token")
                .refreshToken("refresh-token")
                .user(userDTO)
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(response);

        ResponseEntity<UserDTO> result = authenticationController.register(request);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals("Bearer jwt-token", result.getHeaders().getFirst("Authorization"));
        assertEquals("refresh-token", result.getHeaders().getFirst("Refresh-Token"));
        assertEquals(userDTO.getEmail(), result.getBody().getEmail());
    }

    @Test
    void testAuthenticate() {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setEmail("test@email.com");
        request.setPassword("123456");

        UserDTO userDTO = UserDTO.builder().email("test@email.com").fullName("Test User").build();
        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken("jwt-token")
                .refreshToken("refresh-token")
                .user(userDTO)
                .build();

        when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

       /*// ResponseEntity<UserDTO> result = authenticationController.authenticate(request);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals("Bearer jwt-token", result.getHeaders().getFirst("Authorization"));
        assertEquals("refresh-token", result.getHeaders().getFirst("Refresh-Token"));
        assertEquals(userDTO.getEmail(), result.getBody().getEmail());
        */
    }
}

