package com.security.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.security.dto.CompanyRequestDTO;
import com.security.dto.CompanyResponseDTO;
import com.security.model.User;
import com.security.service.CompanyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    
    private final CompanyService companyService;
    
    /**
     * Criar nova empresa
     * POST /api/companies
     * Acesso: Qualquer usuário autenticado pode criar empresa
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponseDTO> createCompany(
            @Valid @RequestBody CompanyRequestDTO dto,
            @AuthenticationPrincipal User currentUser) {
        
        CompanyResponseDTO response = companyService.createCompany(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Atualizar empresa
     * PUT /api/companies/{id}
     * Acesso: Apenas ADMIN da própria empresa ou SUPER_ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponseDTO> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequestDTO dto,
            @AuthenticationPrincipal User currentUser) {
        
        CompanyResponseDTO response = companyService.updateCompany(id, dto, currentUser);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Buscar empresa por ID
     * GET /api/companies/{id}
     * Acesso: Usuários autenticados podem ver qualquer empresa
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponseDTO> getCompany(@PathVariable Long id) {
        CompanyResponseDTO response = companyService.getCompanyById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Buscar empresa por ID com estatísticas
     * GET /api/companies/{id}/stats
     * Acesso: Apenas ADMIN da própria empresa ou SUPER_ADMIN
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponseDTO> getCompanyWithStats(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        CompanyResponseDTO response = companyService.getCompanyByIdWithStats(id, currentUser);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Buscar empresa por NIF
     * GET /api/companies/nif/{nif}
     * Acesso: Usuários autenticados
     */
    @GetMapping("/nif/{nif}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompanyResponseDTO> getCompanyByNif(@PathVariable String nif) {
        CompanyResponseDTO response = companyService.getCompanyByNif(nif);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Listar todas as empresas
     * GET /api/companies
     * Acesso: Usuários autenticados veem lista básica, SUPER_ADMIN vê todas
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompanyResponseDTO>> getAllCompanies(
            @AuthenticationPrincipal User currentUser) {
        
        List<CompanyResponseDTO> companies = companyService.getAllCompanies(currentUser);
        return ResponseEntity.ok(companies);
    }
    
    /**
     * Listar empresas ativas
     * GET /api/companies/active
     * Acesso: Usuários autenticados
     */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompanyResponseDTO>> getActiveCompanies() {
        List<CompanyResponseDTO> companies = companyService.getActiveCompanies();
        return ResponseEntity.ok(companies);
    }
    
    /**
     * Listar empresas inativas
     * GET /api/companies/inactive
     * Acesso: Apenas SUPER_ADMIN
     */
    @GetMapping("/inactive")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CompanyResponseDTO>> getInactiveCompanies() {
        List<CompanyResponseDTO> companies = companyService.getInactiveCompanies();
        return ResponseEntity.ok(companies);
    }
    
    /**
     * Buscar empresas por nome
     * GET /api/companies/search?name={name}
     * Acesso: Usuários autenticados
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompanyResponseDTO>> searchCompanies(
            @RequestParam String name) {
        
        List<CompanyResponseDTO> companies = companyService.searchCompaniesByName(name);
        return ResponseEntity.ok(companies);
    }
    
    /**
     * Buscar empresas por cidade
     * GET /api/companies/city/{city}
     * Acesso: Usuários autenticados
     */
    @GetMapping("/city/{city}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CompanyResponseDTO>> getCompaniesByCity(
            @PathVariable String city) {
        
        List<CompanyResponseDTO> companies = companyService.getCompaniesByCity(city);
        return ResponseEntity.ok(companies);
    }
    
    /**
     * Ativar empresa
     * PATCH /api/companies/{id}/activate
     * Acesso: Apenas SUPER_ADMIN
     */
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponseDTO> activateCompany(@PathVariable Long id) {
        CompanyResponseDTO response = companyService.activateCompany(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Desativar empresa
     * PATCH /api/companies/{id}/deactivate
     * Acesso: Apenas SUPER_ADMIN
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CompanyResponseDTO> deactivateCompany(@PathVariable Long id) {
        CompanyResponseDTO response = companyService.deactivateCompany(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Deletar empresa (verifica relacionamentos)
     * DELETE /api/companies/{id}
     * Acesso: Apenas SUPER_ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Deletar empresa forçadamente (hard delete)
     * DELETE /api/companies/{id}/hard
     * Acesso: Apenas SUPER_ADMIN
     */
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> hardDeleteCompany(@PathVariable Long id) {
        companyService.hardDeleteCompany(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Contar total de empresas
     * GET /api/companies/count/all
     * Acesso: Apenas SUPER_ADMIN
     */
    @GetMapping("/count/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Long> countAllCompanies() {
        long count = companyService.countAllCompanies();
        return ResponseEntity.ok(count);
    }
    
    /**
     * Contar empresas ativas
     * GET /api/companies/count/active
     * Acesso: Usuários autenticados
     */
    @GetMapping("/count/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> countActiveCompanies() {
        long count = companyService.countActiveCompanies();
        return ResponseEntity.ok(count);
    }
}