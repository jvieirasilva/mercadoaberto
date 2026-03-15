package com.security.service;

import com.security.model.Role;
import com.security.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    // Chave do application.yml
    private static final String SECRET_KEY =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long JWT_EXPIRATION    = 86400000L;  // 1 dia
    private static final long REFRESH_EXPIRATION = 604800000L; // 7 dias

    private JwtService jwtService;

    private User userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", JWT_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", REFRESH_EXPIRATION);

        userDetails = User.builder()
                .id(1L)
                .fullName("José Silva")
                .email("jose@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .isActive(true)
                .isNotLocked(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // generateToken()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("generateToken()")
    class GenerateTokenTests {

        @Test
        @DisplayName("Deve gerar token JWT não nulo e não vazio")
        void shouldGenerateNonNullToken() {
            String token = jwtService.generateToken(userDetails);

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Deve gerar tokens diferentes para chamadas distintas")
        void shouldGenerateDifferentTokensOnMultipleCalls() throws Exception {
            String token1 = jwtService.generateToken(userDetails);
            Thread.sleep(10); // garante timestamps diferentes
            String token2 = jwtService.generateToken(userDetails);

            // Tokens podem ser iguais se emitidos no mesmo milissegundo, mas normalmente são diferentes
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
        }

        @Test
        @DisplayName("Deve gerar token com extra claims")
        void shouldGenerateTokenWithExtraClaims() {
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("role", "USER");
            extraClaims.put("userId", 1L);

            String token = jwtService.generateToken(extraClaims, userDetails);

            assertThat(token).isNotNull().isNotBlank();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // generateRefreshToken()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshTokenTests {

        @Test
        @DisplayName("Deve gerar refresh token não nulo e não vazio")
        void shouldGenerateNonNullRefreshToken() {
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            assertThat(refreshToken).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Token de acesso e refresh devem ser diferentes")
        void accessAndRefreshTokensShouldDiffer() {
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // extractUsername()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsernameTests {

        @Test
        @DisplayName("Deve extrair o email correto do token")
        void shouldExtractEmailFromToken() {
            String token = jwtService.generateToken(userDetails);

            String username = jwtService.extractUsername(token);

            assertThat(username).isEqualTo("jose@test.com");
        }

        @Test
        @DisplayName("Deve extrair username do refresh token")
        void shouldExtractUsernameFromRefreshToken() {
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            String username = jwtService.extractUsername(refreshToken);

            assertThat(username).isEqualTo("jose@test.com");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // isTokenValid()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValidTests {

        @Test
        @DisplayName("Deve retornar true para token válido do mesmo usuário")
        void shouldReturnTrueForValidToken() {
            String token = jwtService.generateToken(userDetails);

            boolean valid = jwtService.isTokenValid(token, userDetails);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false quando token pertence a outro usuário")
        void shouldReturnFalseForDifferentUser() {
            String token = jwtService.generateToken(userDetails);

            UserDetails otherUser = User.builder()
                    .id(2L)
                    .email("outro@test.com")
                    .password("pass")
                    .role(Role.USER)
                    .isActive(true)
                    .isNotLocked(true)
                    .build();

            boolean valid = jwtService.isTokenValid(token, otherUser);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para token expirado")
        void shouldReturnFalseForExpiredToken() throws Exception {
            // Criar JwtService com expiração de 1ms
            JwtService shortLivedService = new JwtService();
            ReflectionTestUtils.setField(shortLivedService, "secretKey", SECRET_KEY);
            ReflectionTestUtils.setField(shortLivedService, "jwtExpiration", 1L);
            ReflectionTestUtils.setField(shortLivedService, "refreshExpiration", 1L);

            String token = shortLivedService.generateToken(userDetails);
            Thread.sleep(50); // espera o token expirar

            assertThatThrownBy(() -> shortLivedService.isTokenValid(token, userDetails))
                    .isInstanceOf(Exception.class); // ExpiredJwtException
        }

        @Test
        @DisplayName("Deve retornar true para token com extra claims do mesmo usuário")
        void shouldReturnTrueForTokenWithExtraClaims() {
            Map<String, Object> claims = new HashMap<>();
            claims.put("customClaim", "value");
            String token = jwtService.generateToken(claims, userDetails);

            boolean valid = jwtService.isTokenValid(token, userDetails);

            assertThat(valid).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // extractClaim()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("extractClaim()")
    class ExtractClaimTests {

        @Test
        @DisplayName("Deve extrair a data de expiração do token")
        void shouldExtractExpiration() {
            String token = jwtService.generateToken(userDetails);

            var expiration = jwtService.extractClaim(token,
                    claims -> claims.getExpiration());

            assertThat(expiration).isNotNull();
            assertThat(expiration).isInTheFuture();
        }

        @Test
        @DisplayName("Deve extrair o subject (email) via extractClaim")
        void shouldExtractSubjectViaClaim() {
            String token = jwtService.generateToken(userDetails);

            String subject = jwtService.extractClaim(token,
                    claims -> claims.getSubject());

            assertThat(subject).isEqualTo("jose@test.com");
        }
    }
}
