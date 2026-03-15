package com.security.service;

import com.security.dto.RegisterRequest;
import com.security.dto.UserDTO;
import com.security.model.EmailVerificationToken;
import com.security.model.PasswordResetToken;
import com.security.model.Role;
import com.security.model.User;
import com.security.repository.EmailVerificationTokenRepository;
import com.security.repository.PasswordResetTokenRepository;
import com.security.repository.UserRepository;
import com.security.reqeuest.AuthenticationRequest;
import com.security.response.AuthenticationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock private UserRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private S3UploadService s3UploadService;
    @Mock private EmailSender emailSender;
    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .fullName("José Silva")
                .email("jose@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .isActive(true)
                .isNotLocked(true)
                .isChangePassword(false)
                .joinDate(new Date())
                .build();

        inactiveUser = User.builder()
                .id(2L)
                .fullName("Maria Test")
                .email("maria@test.com")
                .password("encodedPassword")
                .role(Role.USER)
                .isActive(false)
                .isNotLocked(false)
                .joinDate(new Date())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class RegisterTests {

        private RegisterRequest buildRequest(String email, String role) {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("José Silva");
            req.setEmail(email);
            req.setPassword("password123");
            req.setRole(role);
            req.setIsActive(false);
            req.setIsNotLocked(false);
            req.setIsChangePassword(false);
            return req;
        }

        @Test
        @DisplayName("Deve registrar um novo usuário com sucesso e retornar tokens JWT")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            RegisterRequest request = buildRequest("novo@test.com", "USER");
            when(repository.findByEmail("novo@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encodedPass");
            when(repository.save(any(User.class))).thenReturn(activeUser);
            doNothing().when(emailSender).sendConfirmationEmail(any(), any(), any());
            when(tokenRepository.save(any())).thenReturn(null);
            when(jwtService.generateToken(any())).thenReturn("jwt-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

            AuthenticationResponse response = authenticationService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser()).isNotNull();

            verify(repository).save(any(User.class));
            verify(emailSender).sendConfirmationEmail(eq("novo@test.com"), eq("José Silva"), any());
            verify(tokenRepository).save(any(EmailVerificationToken.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando email já existe")
        void shouldThrowWhenEmailAlreadyExists() {
            RegisterRequest request = buildRequest("jose@test.com", "USER");
            when(repository.findByEmail("jose@test.com")).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("email já está cadastrado");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve usar role USER como padrão quando role é inválida")
        void shouldDefaultToUserRoleWhenInvalid() throws Exception {
            RegisterRequest request = buildRequest("novo@test.com", "INVALID_ROLE");
            when(repository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            doNothing().when(emailSender).sendConfirmationEmail(any(), any(), any());
            when(tokenRepository.save(any())).thenReturn(null);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(repository.save(userCaptor.capture())).thenReturn(activeUser);
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            authenticationService.register(request);

            assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("Deve usar role USER como padrão quando role é nula")
        void shouldDefaultToUserRoleWhenNull() throws Exception {
            RegisterRequest request = buildRequest("novo@test.com", null);
            when(repository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            doNothing().when(emailSender).sendConfirmationEmail(any(), any(), any());
            when(tokenRepository.save(any())).thenReturn(null);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(repository.save(userCaptor.capture())).thenReturn(activeUser);
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            authenticationService.register(request);

            assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("Deve lançar exceção quando o email sender falha")
        void shouldThrowWhenEmailSenderFails() throws Exception {
            RegisterRequest request = buildRequest("novo@test.com", "USER");
            when(repository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(repository.save(any(User.class))).thenReturn(activeUser);
            doThrow(new RuntimeException("SMTP error")).when(emailSender)
                    .sendConfirmationEmail(any(), any(), any());

            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Erro ao enviar email");
        }

        @Test
        @DisplayName("Deve criar novo usuário com isActive=false para forçar confirmação de email")
        void shouldCreateUserWithIsActiveFalse() throws Exception {
            RegisterRequest request = buildRequest("novo@test.com", "USER");
            when(repository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("encoded");
            doNothing().when(emailSender).sendConfirmationEmail(any(), any(), any());
            when(tokenRepository.save(any())).thenReturn(null);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            when(repository.save(userCaptor.capture())).thenReturn(activeUser);
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            authenticationService.register(request);

            // O primeiro save cria o user com isActive=false
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.isActive()).isFalse();
            assertThat(savedUser.isNotLocked()).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // authenticate()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("authenticate()")
    class AuthenticateTests {

        @Test
        @DisplayName("Deve autenticar com sucesso e retornar JWT + dados do usuário")
        void shouldAuthenticateSuccessfully() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("jose@test.com")
                    .password("password123")
                    .build();

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(repository.findByEmailWithCompany("jose@test.com")).thenReturn(Optional.of(activeUser));
            when(jwtService.generateToken(activeUser)).thenReturn("jwt-token");
            when(jwtService.generateRefreshToken(activeUser)).thenReturn("refresh-token");

            AuthenticationResponse response = authenticationService.authenticate(request);

            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser().getEmail()).isEqualTo("jose@test.com");
            assertThat(response.getUser().getRole()).isEqualTo("USER");

            verify(authenticationManager).authenticate(
                    argThat(token -> token instanceof UsernamePasswordAuthenticationToken
                            && token.getPrincipal().equals("jose@test.com")));
        }

        @Test
        @DisplayName("Deve lançar exceção quando autenticação falha")
        void shouldThrowWhenAuthenticationFails() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("jose@test.com")
                    .password("wrongpassword")
                    .build();

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new RuntimeException("Bad credentials"));

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Falha na autenticação");
        }

        @Test
        @DisplayName("Deve retornar companyDTO nulo quando usuário não tem empresa")
        void shouldReturnNullCompanyWhenUserHasNoCompany() {
            AuthenticationRequest request = AuthenticationRequest.builder()
                    .email("jose@test.com")
                    .password("password123")
                    .build();

            User userWithoutCompany = User.builder()
                    .id(1L)
                    .email("jose@test.com")
                    .fullName("José")
                    .role(Role.USER)
                    .isActive(true)
                    .isNotLocked(true)
                    .company(null)
                    .build();

            when(authenticationManager.authenticate(any())).thenReturn(null);
            when(repository.findByEmailWithCompany("jose@test.com")).thenReturn(Optional.of(userWithoutCompany));
            when(jwtService.generateToken(any())).thenReturn("token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh");

            AuthenticationResponse response = authenticationService.authenticate(request);

            assertThat(response.getUser().getCompany()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getUserById()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getUserById()")
    class GetUserByIdTests {

        @Test
        @DisplayName("Deve retornar UserDTO quando usuário existe")
        void shouldReturnUserDtoWhenFound() {
            when(repository.findById(1L)).thenReturn(Optional.of(activeUser));

            UserDTO result = authenticationService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("jose@test.com");
            assertThat(result.getFullName()).isEqualTo("José Silva");
            assertThat(result.getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não é encontrado")
        void shouldThrowWhenUserNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.getUserById(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // deleteUser()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteUser()")
    class DeleteUserTests {

        @Test
        @DisplayName("Deve deletar usuário sem imagem com sucesso")
        void shouldDeleteUserWithoutImage() {
            when(repository.findById(1L)).thenReturn(Optional.of(activeUser));

            authenticationService.deleteUser(1L);

            verify(repository).delete(activeUser);
            verify(s3UploadService, never()).deleteProfileImage(any());
        }

        @Test
        @DisplayName("Deve deletar usuário com imagem e remover do S3")
        void shouldDeleteUserWithImageAndRemoveFromS3() {
            activeUser.setProfileImageUrl("https://bucket.s3.us-east-1.amazonaws.com/profile-images/foto.png");
            when(repository.findById(1L)).thenReturn(Optional.of(activeUser));

            authenticationService.deleteUser(1L);

            verify(s3UploadService).deleteProfileImage("foto.png");
            verify(repository).delete(activeUser);
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não existe")
        void shouldThrowWhenUserNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.deleteUser(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");

            verify(repository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // searchUsersByName()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("searchUsersByName()")
    class SearchUsersTests {

        @Test
        @DisplayName("Deve retornar página de usuários filtrados por nome")
        void shouldReturnPagedUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(activeUser), pageable, 1);
            when(repository.searchByNameOrEmail("José", pageable)).thenReturn(userPage);

            Page<UserDTO> result = authenticationService.searchUsersByName("José", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("jose@test.com");
        }

        @Test
        @DisplayName("Deve retornar página vazia quando nenhum usuário é encontrado")
        void shouldReturnEmptyPageWhenNoUsersFound() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(repository.searchByNameOrEmail("xyz", pageable)).thenReturn(emptyPage);

            Page<UserDTO> result = authenticationService.searchUsersByName("xyz", pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // confirmEmail()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("confirmEmail()")
    class ConfirmEmailTests {

        @Test
        @DisplayName("Deve confirmar email e ativar o usuário com sucesso")
        void shouldConfirmEmailAndActivateUser() {
            String token = "valid-token-uuid";
            EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                    .token(token)
                    .email("jose@test.com")
                    .createdDate(new Date())
                    .expiryDate(new Date(System.currentTimeMillis() + 86400000L)) // +24h
                    .used(false)
                    .build();

            when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
            when(repository.findByEmail("jose@test.com")).thenReturn(Optional.of(inactiveUser));
            when(repository.save(any(User.class))).thenReturn(inactiveUser);

            authenticationService.confirmEmail(token);

            verify(repository).save(argThat(u -> u.isActive() && u.isNotLocked()));
            verify(tokenRepository).delete(verificationToken);
        }

        @Test
        @DisplayName("Deve lançar exceção quando token não existe")
        void shouldThrowWhenTokenNotFound() {
            when(tokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.confirmEmail("invalid"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("inválido");
        }

        @Test
        @DisplayName("Deve lançar exceção quando token já foi usado")
        void shouldThrowWhenTokenAlreadyUsed() {
            EmailVerificationToken usedToken = EmailVerificationToken.builder()
                    .token("used-token")
                    .email("jose@test.com")
                    .createdDate(new Date())
                    .expiryDate(new Date(System.currentTimeMillis() + 86400000L))
                    .used(true)
                    .build();

            when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

            assertThatThrownBy(() -> authenticationService.confirmEmail("used-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("já foi utilizado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando token está expirado")
        void shouldThrowWhenTokenExpired() {
            EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                    .token("expired-token")
                    .email("jose@test.com")
                    .createdDate(new Date(System.currentTimeMillis() - 172800000L)) // -48h
                    .expiryDate(new Date(System.currentTimeMillis() - 86400000L))   // -24h
                    .used(false)
                    .build();

            when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authenticationService.confirmEmail("expired-token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expirou");
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário já está ativo")
        void shouldThrowWhenUserAlreadyActive() {
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .token("token123")
                    .email("jose@test.com")
                    .createdDate(new Date())
                    .expiryDate(new Date(System.currentTimeMillis() + 86400000L))
                    .used(false)
                    .build();

            when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(token));
            when(repository.findByEmail("jose@test.com")).thenReturn(Optional.of(activeUser)); // já ativo

            assertThatThrownBy(() -> authenticationService.confirmEmail("token123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("já foi confirmado");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // requestPasswordReset()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("requestPasswordReset()")
    class RequestPasswordResetTests {

        @Test
        @DisplayName("Deve gerar token e enviar email de reset com sucesso")
        void shouldGenerateTokenAndSendEmail() {
            when(repository.findByEmail("jose@test.com")).thenReturn(Optional.of(activeUser));
            doNothing().when(passwordResetTokenRepository).deleteByEmail("jose@test.com");
            when(passwordResetTokenRepository.save(any())).thenReturn(null);
            doNothing().when(emailSender).sendPasswordResetEmail(any(), any(), any());

            authenticationService.requestPasswordReset("jose@test.com");

            verify(passwordResetTokenRepository).deleteByEmail("jose@test.com");
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            verify(emailSender).sendPasswordResetEmail(eq("jose@test.com"), eq("José Silva"), any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não existe")
        void shouldThrowWhenUserNotFound() {
            when(repository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.requestPasswordReset("nonexistent@test.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Deve lançar exceção quando email sender falha")
        void shouldThrowWhenEmailFails() {
            when(repository.findByEmail("jose@test.com")).thenReturn(Optional.of(activeUser));
            doNothing().when(passwordResetTokenRepository).deleteByEmail(any());
            when(passwordResetTokenRepository.save(any())).thenReturn(null);
            doThrow(new RuntimeException("SMTP error"))
                    .when(emailSender).sendPasswordResetEmail(any(), any(), any());

            assertThatThrownBy(() -> authenticationService.requestPasswordReset("jose@test.com"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // resetPassword()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resetPassword()")
    class ResetPasswordTests {

        @Test
        @DisplayName("Deve resetar a senha com sucesso")
        void shouldResetPasswordSuccessfully() {
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token("reset-token")
                    .email("jose@test.com")
                    .createdDate(new Date())
                    .expiryDate(new Date(System.currentTimeMillis() + 3600000L)) // +1h
                    .used(false)
                    .build();

            when(passwordResetTokenRepository.findByToken("reset-token"))
                    .thenReturn(Optional.of(resetToken));
            when(repository.findByEmail("jose@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
            when(repository.save(any())).thenReturn(activeUser);
            when(passwordResetTokenRepository.save(any())).thenReturn(resetToken);

            authenticationService.resetPassword("reset-token", "newPassword123");

            verify(repository).save(argThat(u -> u.getPassword().equals("encodedNewPassword")));
            verify(passwordResetTokenRepository).save(argThat(t -> t.isUsed()));
        }

        @Test
        @DisplayName("Deve lançar exceção quando token não existe")
        void shouldThrowWhenTokenNotFound() {
            when(passwordResetTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.resetPassword("invalid", "newPass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid or expired");
        }

        @Test
        @DisplayName("Deve lançar exceção quando token já foi usado")
        void shouldThrowWhenTokenAlreadyUsed() {
            PasswordResetToken usedToken = PasswordResetToken.builder()
                    .token("used-token")
                    .email("jose@test.com")
                    .createdDate(new Date())
                    .expiryDate(new Date(System.currentTimeMillis() + 3600000L))
                    .used(true)
                    .build();

            when(passwordResetTokenRepository.findByToken("used-token"))
                    .thenReturn(Optional.of(usedToken));

            assertThatThrownBy(() -> authenticationService.resetPassword("used-token", "newPass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already been used");
        }

        @Test
        @DisplayName("Deve lançar exceção quando token está expirado")
        void shouldThrowWhenTokenExpired() {
            PasswordResetToken expiredToken = PasswordResetToken.builder()
                    .token("expired-token")
                    .email("jose@test.com")
                    .createdDate(new Date(System.currentTimeMillis() - 7200000L))
                    .expiryDate(new Date(System.currentTimeMillis() - 3600000L)) // expirou há 1h
                    .used(false)
                    .build();

            when(passwordResetTokenRepository.findByToken("expired-token"))
                    .thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authenticationService.resetPassword("expired-token", "newPass"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("expired");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // createDefaultUserIfNotExists()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createDefaultUserIfNotExists()")
    class CreateDefaultUserTests {

        @Test
        @DisplayName("Deve criar usuário quando não existe")
        void shouldCreateUserWhenNotExists() {
            when(repository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("admin123")).thenReturn("encodedAdmin");
            when(repository.save(any(User.class))).thenReturn(activeUser);

            UserDTO result = authenticationService.createDefaultUserIfNotExists(
                    "admin@test.com", "admin123", "Admin User");

            verify(repository).save(any(User.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve retornar usuário existente sem criar novo")
        void shouldReturnExistingUserWithoutCreating() {
            when(repository.findByEmail("jose@test.com")).thenReturn(Optional.of(activeUser));

            UserDTO result = authenticationService.createDefaultUserIfNotExists(
                    "jose@test.com", "pass", "José");

            verify(repository, never()).save(any());
            assertThat(result.getEmail()).isEqualTo("jose@test.com");
        }
    }
}
