package com.security.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.security.dto.RegisterRequest;
import com.security.dto.UserDTO;
import com.security.reqeuest.AuthenticationRequest;
import com.security.response.AuthenticationResponse;
import com.security.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para autenticação e registro de usuários")
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    private final KafkaTemplate<String, RegisterRequest> kafkaTemplate;
    
    @PostMapping(
    		  path = "/register",
    		  consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    		  produces = MediaType.APPLICATION_JSON_VALUE
 	)
    @Operation(
        summary = "Registrar novo usuário",
        description = "Cria um novo usuário no sistema e retorna um token JWT válido"
    )
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserDTO> register(@ModelAttribute RegisterRequest request) throws IOException {
        AuthenticationResponse response = authenticationService.register(request);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.getAccessToken())
                .header("Refresh-Token", response.getRefreshToken())
                .body(response.getUser()); 
    }
    
    @PostMapping("/registerKafka")
    @Operation(
        summary = "Registrar novo usuário via Kafka",
        description = "Envia os dados do usuário para um tópico Kafka para ser processado"
    )
    public ResponseEntity<String> registerKafka(@RequestBody RegisterRequest request) {
        kafkaTemplate.send("user-register-topic", request); 
        return ResponseEntity.accepted().body("Usuário enviado para o tópico Kafka com sucesso");
    }

    @PostMapping("/authenticate")
    @Operation(
        summary = "Autenticar usuário",
        description = "Autentica o usuário com email e senha e retorna um token JWT válido"
    )
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = authenticationService.authenticate(request);
        
        // ✅ Retornar objeto completo no body (não nos headers)
        return ResponseEntity.ok(response);
    }
  
    
    @GetMapping("/users/search")
    @Operation(
        summary = "Pesquisar usuários por nome",
        description = "Retorna uma lista paginada de usuários filtrados por nome (fullName ou email)"
    )
    public ResponseEntity<Page<UserDTO>> searchUsers(
            @Parameter(description = "Nome ou email do usuário para filtrar")
            @RequestParam(required = false, defaultValue = "") String name,
            
            @Parameter(description = "Número da página (começa em 0)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Tamanho da página")
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "Campo para ordenação (ex: fullName, email, joinDate)")
            @RequestParam(defaultValue = "fullName") String sortBy,
            
            @Parameter(description = "Direção da ordenação (ASC ou DESC)")
            @RequestParam(defaultValue = "ASC") String direction
    ) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") 
            ? Sort.Direction.DESC 
            : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<UserDTO> users = authenticationService.searchUsersByName(name, pageable);
        
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/{id}")
    @Operation(
        summary = "Buscar usuário por ID",
        description = "Retorna os dados de um usuário específico pelo ID"
    )
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "ID do usuário")
            @PathVariable Long id // ✅ Long (não Integer)
    ) {
        UserDTO user = authenticationService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping(value = "/users/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Atualizar usuário",
        description = "Atualiza os dados de um usuário existente, incluindo a foto de perfil"
    )
    public ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "ID do usuário a ser atualizado")
            @PathVariable Long id, // ✅ Long (não Integer)
            @ModelAttribute RegisterRequest request
    ) throws IOException {
        UserDTO updatedUser = authenticationService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/users/{id}")
    @Operation(
        summary = "Deletar usuário",
        description = "Remove um usuário do sistema (exclusão lógica ou física)"
    )
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID do usuário a ser deletado")
            @PathVariable Long id
    ) {
        authenticationService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/confirm-email")
    @Operation(
        summary = "Confirmar email do usuário",
        description = "Ativa a conta do usuário após confirmação do email"
    )
    public ResponseEntity<Map<String, String>> confirmEmail(
        @Parameter(description = "Token de confirmação enviado por email")
        @RequestParam String token
    ) {
        try {
            authenticationService.confirmEmail(token);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email confirmado com sucesso! Você já pode fazer login.");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request password reset",
        description = "Sends a password reset link to the user's email"
    )
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Email is required");
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }

            authenticationService.requestPasswordReset(email);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset link has been sent to your email");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Resets user password using the token sent via email"
    )
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            
            if (token == null || token.trim().isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Reset token is required");
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (newPassword == null || newPassword.length() < 6) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Password must be at least 6 characters long");
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }
            
            authenticationService.resetPassword(token, newPassword);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password has been reset successfully! You can now login with your new password.");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.badRequest().body(response);
        }
    }

}