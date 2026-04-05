package com.security.filter;

import com.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @InjectMocks private JwtAuthenticationFilter filter;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @Mock private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Sem header Authorization")
    class SemHeaderTests {

        @Test
        @DisplayName("deve continuar chain sem header")
        void shouldContinueWithoutHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("deve continuar chain com header inválido")
        void shouldContinueWithInvalidHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic abc123");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtService);
        }
    }

    @Nested
    @DisplayName("Com header Authorization válido")
    class ComHeaderTests {

        @Test
        @DisplayName("deve autenticar utilizador com token válido")
        void shouldAuthenticateWithValidToken() throws Exception {
            String token = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn("jose@test.com");
            when(userDetailsService.loadUserByUsername("jose@test.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(jwtService).extractUsername(token);
            verify(jwtService).isTokenValid(token, userDetails);
        }

        @Test
        @DisplayName("deve ignorar token inválido")
        void shouldSkipInvalidToken() throws Exception {
            String token = "invalid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn("jose@test.com");
            when(userDetailsService.loadUserByUsername("jose@test.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("deve ignorar quando username é null")
        void shouldSkipWhenUsernameIsNull() throws Exception {
            String token = "token.sem.username";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(jwtService.extractUsername(token)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userDetailsService);
        }
    }
}
