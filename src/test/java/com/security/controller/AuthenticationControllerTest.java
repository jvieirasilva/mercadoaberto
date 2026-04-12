package com.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.security.dto.RegisterRequest;
import com.security.dto.UserDTO;
import com.security.reqeuest.AuthenticationRequest;
import com.security.response.AuthenticationResponse;
import com.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private KafkaTemplate<String, RegisterRequest> kafkaTemplate;
    @InjectMocks private AuthenticationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        objectMapper = new ObjectMapper();
    }

    private Page<UserDTO> buildPage(UserDTO item) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "fullName"));
        return new PageImpl<>(List.of(item), pageable, 1);
    }

    @Nested
    @DisplayName("POST /api/auth/authenticate")
    class AuthenticateTests {

        @Test
        @DisplayName("deve autenticar utilizador com sucesso")
        void shouldAuthenticate() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest();
            request.setEmail("jose@test.com");
            request.setPassword("password123");

            UserDTO userDTO = new UserDTO();
            userDTO.setEmail("jose@test.com");

            AuthenticationResponse authResponse = new AuthenticationResponse();
            authResponse.setAccessToken("access-token");
            authResponse.setRefreshToken("refresh-token");
            authResponse.setUser(userDTO);

            when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post("/api/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("deve retornar utilizador por ID")
        void shouldGetUserById() throws Exception {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(1L);
            userDTO.setEmail("jose@test.com");

            when(authenticationService.getUserById(1L)).thenReturn(userDTO);

            mockMvc.perform(get("/api/auth/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/users/search")
    class SearchUsersTests {

        @Test
        @DisplayName("deve pesquisar utilizadores por nome")
        void shouldSearchUsers() throws Exception {
            UserDTO userDTO = new UserDTO();
            userDTO.setEmail("jose@test.com");

            // Corrigido: searchUsersByName agora recebe (String, Long, Pageable)
            when(authenticationService.searchUsersByName(anyString(), anyLong(), any(Pageable.class)))
                    .thenReturn(buildPage(userDTO));

            mockMvc.perform(get("/api/auth/users/search").param("name", "jose"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("DELETE /api/auth/users/{id}")
    class DeleteUserTests {

        @Test
        @DisplayName("deve deletar utilizador")
        void shouldDeleteUser() throws Exception {
            doNothing().when(authenticationService).deleteUser(1L);

            mockMvc.perform(delete("/api/auth/users/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/auth/confirm-email")
    class ConfirmEmailTests {

        @Test
        @DisplayName("deve confirmar email com token valido")
        void shouldConfirmEmail() throws Exception {
            doNothing().when(authenticationService).confirmEmail("valid-token");

            mockMvc.perform(get("/api/auth/confirm-email").param("token", "valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("deve retornar erro com token invalido")
        void shouldReturnErrorForInvalidToken() throws Exception {
            doThrow(new RuntimeException("Token invalido")).when(authenticationService).confirmEmail("bad-token");

            mockMvc.perform(get("/api/auth/confirm-email").param("token", "bad-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("deve enviar link de reset com email valido")
        void shouldSendResetLink() throws Exception {
            doNothing().when(authenticationService).requestPasswordReset("jose@test.com");

            mockMvc.perform(post("/api/auth/forgot-password").param("email", "jose@test.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("deve retornar erro com email vazio")
        void shouldReturnErrorForEmptyEmail() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password").param("email", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("deve retornar erro quando servico lanca excecao")
        void shouldReturnErrorWhenServiceThrows() throws Exception {
            doThrow(new RuntimeException("Email nao encontrado"))
                    .when(authenticationService).requestPasswordReset("nao@existe.com");

            mockMvc.perform(post("/api/auth/forgot-password").param("email", "nao@existe.com"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("deve resetar password com token valido")
        void shouldResetPassword() throws Exception {
            Map<String, String> body = Map.of("token", "valid-token", "newPassword", "NewPass123");
            doNothing().when(authenticationService).resetPassword("valid-token", "NewPass123");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("deve retornar erro sem token")
        void shouldReturnErrorWithoutToken() throws Exception {
            Map<String, String> body = Map.of("newPassword", "NewPass123");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("deve retornar erro com password curta")
        void shouldReturnErrorWithShortPassword() throws Exception {
            Map<String, String> body = Map.of("token", "valid-token", "newPassword", "123");

            mockMvc.perform(post("/api/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }
}