package com.security.service;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

import jakarta.transaction.Transactional;

 

@Service
public class AuthenticationService {

	private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final S3UploadService s3UploadService; 
    private final EmailSender emailSender;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);
    
    public AuthenticationService(UserRepository repository, 
            PasswordEncoder passwordEncoder, 
            JwtService jwtService, 
            AuthenticationManager authenticationManager,
            S3UploadService s3UploadService,
            EmailSender emailSender,
            EmailVerificationTokenRepository tokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
					this.repository = repository;
					this.passwordEncoder = passwordEncoder;
					this.jwtService = jwtService;
					this.authenticationManager = authenticationManager;
					this.s3UploadService = s3UploadService;
					this.emailSender = emailSender;
					this.tokenRepository = tokenRepository;
					this.passwordResetTokenRepository = passwordResetTokenRepository;
			}

    public AuthenticationResponse register(RegisterRequest request) throws IOException {

        LOGGER.info("Jose id:" + request.getFullName());
        // ✅ VERIFICAR SE EMAIL JÁ EXISTE
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            LOGGER.error("❌ Email já cadastrado: " + request.getEmail());
            throw new RuntimeException("Este email já está cadastrado. Por favor, use outro email ou faça login.");
        }
        Role userRole = Role.USER; // default
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                userRole = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid role: " + request.getRole() + ". Using default USER role.");
                userRole = Role.USER;
            }
        }

        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(userRole)
            .joinDate(new Date())
            .isActive(false)
            .isNotLocked(false)
            .isChangePassword(request.getIsChangePassword())
            .build();
        repository.save(user);
        
        String testToken = UUID.randomUUID().toString();
        
        
        try {
        	emailSender.sendConfirmationEmail(request.getEmail(), request.getFullName(), testToken);
            LOGGER.info("✅ Email reenviado com sucesso para: " + request.getEmail());
        } catch (Exception e) {
            LOGGER.error("❌ Erro ao reenviar email: " + e.getMessage());
            throw new RuntimeException("Erro ao enviar email de confirmação");
        }
        
        String newToken = createEmailVerificationToken(request.getEmail(),testToken);
        

        
        try {
            if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
                String originalFilename = request.getProfileImage().getOriginalFilename();
                LOGGER.info("Nome original do arquivo: " + originalFilename);
                
                // Separar nome do arquivo e extensão
                String fileNameWithoutExtension = "";
                String fileExtension = "";
                
                if (originalFilename != null && originalFilename.contains(".")) {
                    int lastDotIndex = originalFilename.lastIndexOf(".");
                    fileNameWithoutExtension = originalFilename.substring(0, lastDotIndex);
                    fileExtension = originalFilename.substring(lastDotIndex); // Inclui o ponto
                } else {
                    fileNameWithoutExtension = originalFilename != null ? originalFilename : "profile";
                    fileExtension = ".png";
                }
                
                LOGGER.info("Nome sem extensão: " + fileNameWithoutExtension);
                LOGGER.info("Extensão: " + fileExtension);
                
                // Formato: jose_200.png
               // String newFilename = fileNameWithoutExtension + "_" + user.getId() + fileExtension;
                String newFilename =  user.getId() + fileExtension;
                
                LOGGER.info("Nome final a ser enviado para S3: " + newFilename);
                
                String imageUrl = s3UploadService.uploadProfileImage(
                    newFilename,
                    request.getProfileImage()
                );
                
                LOGGER.info("URL retornada do S3: " + imageUrl);
                
                user.setProfileImageUrl(imageUrl);
                user = repository.save(user);
            }
            LOGGER.info("Gravou o arquivo para user: " + user.getId());
            
        } catch (Exception e) {
            LOGGER.error("Erro ao gravar o arquivo para user: " + user.getId(), e);
        }

        // Geração dos tokens JWT
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        // Criando o DTO do usuário
        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginDateDisplay(user.getLastLoginDateDisplay())
                .joinDate(user.getJoinDate())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .isNotLocked(user.isNotLocked())
                .isChangePassword(user.isChangePassword())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(userDTO)
                .build();
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            var user = repository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou Inactivo/Bloqueado"));
            
            
            

            var jwtToken = jwtService.generateToken(user);
            var refreshToken = jwtService.generateRefreshToken(user);

            UserDTO userDTO = UserDTO.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .profileImageUrl(user.getProfileImageUrl())
                    .lastLoginDate(user.getLastLoginDate())
                    .lastLoginDateDisplay(user.getLastLoginDateDisplay())
                    .joinDate(user.getJoinDate())
                    .role(user.getRole().name())
                    //.authorities(user.getAuthorities())
                    .isActive(user.isActive())
                    .isNotLocked(user.isNotLocked())
                    .isChangePassword(user.isChangePassword())
                    .build();
            
            

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .user(userDTO)
                    .build();
        } catch (Exception e) {
        	 e.printStackTrace();
            throw new RuntimeException("Falha na autenticação", e);
        }
    }
    
    
    public UserDTO createDefaultUserIfNotExists(String email, String rawPassword, String fullName) {
        return repository.findByEmail(email)
                .map(user -> {
                    LOGGER.info("Usuário padrão já existe: {}", email);
                    return toDTO(user);
                })
                .orElseGet(() -> {
                    User user = User.builder()
                            .fullName(fullName)
                            .email(email)
                            .password(passwordEncoder.encode(rawPassword))
                            .role(Role.USER)
                            .isActive(true)
                            .isNotLocked(true)
                            .isChangePassword(false)
                            .joinDate(new Date())
                            .lastLoginDate(new Date())
                            .lastLoginDateDisplay(new Date())
                            .build();

                    repository.save(user);
                    LOGGER.info("Usuário padrão criado com sucesso: {}", email);
                    return toDTO(user);
                });
    }
    
    /**
     * Pesquisa usuários por nome ou email com paginação
     * 
     * @param searchTerm Termo de busca (nome ou email)
     * @param pageable Informações de paginação e ordenação
     * @return Página de UserDTO
     */
    public Page<UserDTO> searchUsersByName(String searchTerm, Pageable pageable) {
        LOGGER.info("Pesquisando usuários com termo: '{}', página: {}, tamanho: {}", 
                    searchTerm, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> users = repository.searchByNameOrEmail(searchTerm, pageable);
        
        // Converter Page<User> para Page<UserDTO>
        Page<UserDTO> userDTOs = users.map(this::toDTO);
        
        LOGGER.info("Encontrados {} usuários na página {} de {}", 
                    userDTOs.getNumberOfElements(), 
                    userDTOs.getNumber() + 1, 
                    userDTOs.getTotalPages());
        
        return userDTOs;
    }
    
    /**
     * Busca usuário por ID
     */
    public UserDTO getUserById(Long id) { // ✅ ALTERADO DE Long PARA Integer
        LOGGER.info("Buscando usuário com ID: {}", id);
        
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
        
        return toDTO(user);
    }
    
    /**
     * Atualiza um usuário existente
     */
    public UserDTO updateUser(Long id, RegisterRequest request) throws IOException { // ✅ ALTERADO DE Long PARA Integer
        LOGGER.info("Atualizando usuário com ID: {}", id);
        
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
        
        // Atualizar campos apenas se fornecidos
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            // Verificar se o email já existe em outro usuário
            repository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(id)) {
                    throw new RuntimeException("Email já está em uso por outro usuário");
                }
            });
            user.setEmail(request.getEmail());
        }
        
        // Atualizar senha apenas se fornecida
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // Atualizar role
        if (request.getRole() != null) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                LOGGER.error("Role inválida: {}", request.getRole());
            }
        }
        
        // Atualizar flags
        if (request.getIsActive() != null) {
            user.setActive(request.getIsActive());
        }
        
        if (request.getIsNotLocked() != null) {
            user.setNotLocked(request.getIsNotLocked());
        }
        
        if (request.getIsChangePassword() != null) {
            user.setChangePassword(request.getIsChangePassword());
        }
        
        // Atualizar imagem de perfil se fornecida
        if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            try {
                // Deletar imagem antiga se existir
                if (user.getProfileImageUrl() != null) {
                    String oldFilename = extractFilenameFromUrl(user.getProfileImageUrl());
                    if (oldFilename != null) {
                        s3UploadService.deleteProfileImage(oldFilename);
                    }
                }
                
                String originalFilename = request.getProfileImage().getOriginalFilename();
                String fileNameWithoutExtension = "";
                String fileExtension = "";
                
                if (originalFilename != null && originalFilename.contains(".")) {
                    int lastDotIndex = originalFilename.lastIndexOf(".");
                    fileNameWithoutExtension = originalFilename.substring(0, lastDotIndex);
                    fileExtension = originalFilename.substring(lastDotIndex);
                } else {
                    fileNameWithoutExtension = originalFilename != null ? originalFilename : "profile";
                    fileExtension = ".png";
                }
                
                String newFilename = fileNameWithoutExtension + "_" + user.getId() + fileExtension;
                
                String imageUrl = s3UploadService.uploadProfileImage(
                    newFilename,
                    request.getProfileImage()
                );
                
                user.setProfileImageUrl(imageUrl);
                LOGGER.info("Imagem atualizada: {}", imageUrl);
                
            } catch (Exception e) {
                LOGGER.error("Erro ao atualizar imagem do usuário {}: {}", id, e.getMessage());
            }
        }
        
        user = repository.save(user);
        LOGGER.info("Usuário atualizado com sucesso: {}", id);
        
        return toDTO(user);
    }
    
    /**
     * Deleta um usuário (exclusão física)
     * Alternativamente, pode fazer exclusão lógica setando isActive = false
     */
    public void deleteUser(Long id) { // ✅ MUDAR DE Integer PARA Long
        LOGGER.info("Deletando usuário com ID: {}", id);
        
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
        
        // Deletar imagem do S3 se existir
        if (user.getProfileImageUrl() != null) {
            try {
                String filename = extractFilenameFromUrl(user.getProfileImageUrl());
                if (filename != null) {
                    s3UploadService.deleteProfileImage(filename);
                    LOGGER.info("Imagem deletada do S3: {}", filename);
                }
            } catch (Exception e) {
                LOGGER.error("Erro ao deletar imagem do S3: {}", e.getMessage());
            }
        }
        
        // Exclusão física
        repository.delete(user);
        
        LOGGER.info("Usuário deletado com sucesso: {}", id);
    }
    
    /**
     * Confirma o email do usuário usando o token
     * 
     * @param token Token de verificação recebido por email
     * @throws RuntimeException se token for inválido, expirado ou já usado
     */
    @Transactional
    public void confirmEmail(String token) {
        LOGGER.info("🔍 Confirmando email com token: " + token);
        
        // ✅ BUSCAR TOKEN NO BANCO
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> {
                LOGGER.error("❌ Token não encontrado: " + token);
                return new RuntimeException("Token inválido ou expirado");
            });
        
        // ✅ VERIFICAR SE JÁ FOI USADO
        if (verificationToken.isUsed()) {
            LOGGER.error("❌ Token já foi usado: " + token);
            throw new RuntimeException("Este link de confirmação já foi utilizado");
        }
        
        // ✅ VERIFICAR SE EXPIROU (24 horas)
        Date now = new Date();
        if (verificationToken.getExpiryDate().before(now)) {
            LOGGER.error("❌ Token expirado: " + token);
            throw new RuntimeException("Este link de confirmação expirou. Solicite um novo email de confirmação");
        }
        
        // ✅ BUSCAR USUÁRIO PELO EMAIL
        User user = repository.findByEmail(verificationToken.getEmail())
            .orElseThrow(() -> {
                LOGGER.error("❌ Usuário não encontrado: " + verificationToken.getEmail());
                return new RuntimeException("Usuário não encontrado");
            });
        
        // ✅ VERIFICAR SE JÁ ESTÁ ATIVO
        if (user.isActive()) {
            LOGGER.warn("⚠️ Usuário já está ativo: " + user.getEmail());
            // Deletar token mesmo assim
            tokenRepository.delete(verificationToken);
            throw new RuntimeException("Este email já foi confirmado anteriormente");
        }
        
        // ✅ ATIVAR USUÁRIO
        user.setActive(true);
        user.setNotLocked(true);
        repository.save(user);
        
        LOGGER.info("✅ Usuário ativado:");
        LOGGER.info("   Email: " + user.getEmail());
        LOGGER.info("   User ID: " + user.getId());
        LOGGER.info("   isActive: " + user.isActive());
        LOGGER.info("   isNotLocked: " + user.isNotLocked());
        
        // ✅ DELETAR TOKEN DA TABELA
        tokenRepository.delete(verificationToken);
        LOGGER.info("🗑️  Token deletado da tabela email_verification_token");
        
        LOGGER.info("✅ Email confirmado com sucesso para: " + user.getEmail());
    }
    
    @Transactional
    public void requestPasswordReset(String email) {
        LOGGER.info("📧 Solicitação de reset de senha para: " + email);
        
        // Verificar se usuário existe
        User user = repository.findByEmail(email)
            .orElseThrow(() -> {
                LOGGER.error("❌ Usuário não encontrado: " + email);
                return new RuntimeException("User not found with this email");
            });
        
        // Deletar tokens antigos deste email
        passwordResetTokenRepository.deleteByEmail(email);
        LOGGER.info("🗑️  Tokens antigos deletados");
        
        // Gerar novo token
        String token = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.HOURS.toMillis(1)); // Válido por 1 hora
        
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .email(email)
            .createdDate(now)
            .expiryDate(expiryDate)
            .used(false)
            .build();
        
        passwordResetTokenRepository.save(resetToken);
        
        LOGGER.info("✅ Token de reset criado:");
        LOGGER.info("   Token: " + token);
        LOGGER.info("   Email: " + email);
        LOGGER.info("   Expira em: " + expiryDate);
        
        // Enviar email
        try {
            emailSender.sendPasswordResetEmail(email, user.getFullName(), token);
            LOGGER.info("📧 Email de reset enviado com sucesso!");
        } catch (Exception e) {
            LOGGER.error("❌ Erro ao enviar email: " + e.getMessage());
            throw new RuntimeException("Failed to send password reset email");
        }
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        LOGGER.info("🔐 Resetando senha com token: " + token);
        
        // Buscar token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> {
                LOGGER.error("❌ Token não encontrado: " + token);
                return new RuntimeException("Invalid or expired reset token");
            });
        
        // Verificar se já foi usado
        if (resetToken.isUsed()) {
            LOGGER.error("❌ Token já foi usado: " + token);
            throw new RuntimeException("This reset link has already been used");
        }
        
        // Verificar se expirou
        Date now = new Date();
        if (resetToken.getExpiryDate().before(now)) {
            LOGGER.error("❌ Token expirado: " + token);
            throw new RuntimeException("This reset link has expired. Please request a new one");
        }
        
        // Buscar usuário
        User user = repository.findByEmail(resetToken.getEmail())
            .orElseThrow(() -> {
                LOGGER.error("❌ Usuário não encontrado: " + resetToken.getEmail());
                return new RuntimeException("User not found");
            });
        
        // Atualizar senha
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        
        // Marcar token como usado
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        LOGGER.info("✅ Senha resetada com sucesso para: " + user.getEmail());
    }

    
    
    
    
    /**
     * Extrai o nome do arquivo da URL do S3
     */
    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // URL format: https://bucket.s3.region.amazonaws.com/profile-images/filename.png
        String[] parts = url.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        
        return null;
    }
    
   
    
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginDateDisplay(user.getLastLoginDateDisplay())
                .joinDate(user.getJoinDate())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .isNotLocked(user.isNotLocked())
                .isChangePassword(user.isChangePassword())
                .build();
    }
    
    /**
     * Cria um token de verificação de email e salva no banco de dados
     * 
     * @param email Email do usuário
     * @return Token UUID gerado
     */
    private String createEmailVerificationToken(String email,String token) {
        LOGGER.info("Gerando token de verificação para: " + email);
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TimeUnit.HOURS.toMillis(24));
        
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
            .token(token)
            .email(email)
            .createdDate(now)
            .expiryDate(expiryDate)
            .used(false)
            .build();
        
        // ✅ SALVAR NO BANCO
        tokenRepository.save(verificationToken);
        
        LOGGER.info("✅ Token salvo no banco de dados");
        LOGGER.info("   Token: " + token);
        LOGGER.info("   Email: " + email);
        LOGGER.info("   Expira em: " + expiryDate);
        
        return token;
    }
    


}