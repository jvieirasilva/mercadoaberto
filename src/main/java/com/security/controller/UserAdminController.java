package com.security.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.security.dto.RegisterRequest;
import com.security.dto.RegisterRequestAdmin;
import com.security.dto.UserDTO;
import com.security.response.AuthenticationResponse;
import com.security.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/useradmin")
@RequiredArgsConstructor
@Tag(name = "AdminUser", description = "Endpoints para criação de usuários pela empresa (apenas ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')") // ✅ Apenas ADMIN pode acessar
public class UserAdminController {
    
    private final AuthenticationService authenticationService;
    private final KafkaTemplate<String, RegisterRequestAdmin> kafkaTemplate;
    
    @PostMapping(
          path = "/registerAdmin",
          consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
        summary = "Registrar novo usuário da empresa",
        description = "Cria um novo usuário no sistema (apenas ADMIN). O companyId deve ser informado."
    )
    public ResponseEntity<?> registerAdmin(@ModelAttribute RegisterRequestAdmin request) {
        try {
            // Validações básicas
            if (request.getCompanyId() == null || request.getCompanyId().trim().isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Company ID is required");
                
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(errorResponse);
            }
            
            AuthenticationResponse response = authenticationService.registerAdmin(request);

            return ResponseEntity.ok()
                    .header("Authorization", "Bearer " + response.getAccessToken())
                    .header("Refresh-Token", response.getRefreshToken())
                    .body(response.getUser()); 
                    
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);
                    
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Erro ao processar imagem");
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    
    @GetMapping("/users/{id}")
    @Operation(
        summary = "Buscar usuário por ID",
        description = "Retorna os dados de um usuário específico pelo ID (apenas ADMIN)"
    )
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "ID do usuário")
            @PathVariable Long id
    ) {
        UserDTO user = authenticationService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping(value = "/users/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Atualizar usuário",
        description = "Atualiza os dados de um usuário existente, incluindo a foto de perfil (apenas ADMIN)"
    )
    public ResponseEntity<?> updateUser(
            @Parameter(description = "ID do usuário a ser atualizado")
            @PathVariable Long id,
            @ModelAttribute RegisterRequest request
    ) {
        try {
            UserDTO updatedUser = authenticationService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
            
        } catch (RuntimeException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);
                    
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Erro ao processar imagem");
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}