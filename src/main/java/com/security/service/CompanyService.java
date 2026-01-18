package com.security.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.security.dto.CompanyRequestDTO;
import com.security.dto.CompanyResponseDTO;
import com.security.model.Company;
import com.security.model.Role;
import com.security.model.User;
import com.security.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyService {
    
    private final CompanyRepository companyRepository;
    
    /**
     * Criar nova empresa
     */
    @Transactional
    public CompanyResponseDTO createCompany(CompanyRequestDTO dto) {
        // Validar se NIF já existe
        if (companyRepository.existsByNif(dto.getNif())) {
            throw new RuntimeException("Company with NIF " + dto.getNif() + " already exists");
        }
        
        // Validar se email já existe (se fornecido)
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            companyRepository.findByEmail(dto.getEmail()).ifPresent(c -> {
                throw new RuntimeException("Company with email " + dto.getEmail() + " already exists");
            });
        }
        
        // Criar entity a partir do DTO
        Company company = new Company();
        company.setCompanyName(dto.getCompanyName());
        company.setTradeName(dto.getTradeName());
        company.setNif(dto.getNif());
        company.setEmail(dto.getEmail());
        company.setPhone(dto.getPhone());
        company.setAddress(dto.getAddress());
        company.setPostalCode(dto.getPostalCode());
        company.setCity(dto.getCity());
        company.setCountry(dto.getCountry() != null ? dto.getCountry() : "Portugal");
        company.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        // Salvar
        Company savedCompany = companyRepository.save(company);
        
        // Retornar DTO
        return toResponseDTO(savedCompany, false);
    }
    
    /**
     * Atualizar empresa existente (com verificação de permissão)
     */
    @Transactional
    public CompanyResponseDTO updateCompany(Long id, CompanyRequestDTO dto, User currentUser) {
        // Buscar empresa existente
        Company existingCompany = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        
        // Verificar permissão: ADMIN só pode editar sua própria empresa
        if (currentUser.getRole() == Role.ADMIN) {
            if (currentUser.getCompany() == null || !currentUser.getCompany().getId().equals(id)) {
                throw new RuntimeException("You do not have permission to update this company");
            }
        }
        // SUPER_ADMIN pode editar qualquer empresa
        
        // Validar se NIF mudou e se já existe outro com o mesmo NIF
        if (!existingCompany.getNif().equals(dto.getNif())) {
            if (companyRepository.existsByNifAndIdNot(dto.getNif(), id)) {
                throw new RuntimeException("Company with NIF " + dto.getNif() + " already exists");
            }
        }
        
        // Validar email único (se mudou e foi fornecido)
        if (dto.getEmail() != null && !dto.getEmail().isBlank() 
                && !dto.getEmail().equals(existingCompany.getEmail())) {
            companyRepository.findByEmail(dto.getEmail()).ifPresent(c -> {
                if (!c.getId().equals(id)) {
                    throw new RuntimeException("Company with email " + dto.getEmail() + " already exists");
                }
            });
        }
        
        // Atualizar entity
        existingCompany.setCompanyName(dto.getCompanyName());
        existingCompany.setTradeName(dto.getTradeName());
        existingCompany.setNif(dto.getNif());
        existingCompany.setEmail(dto.getEmail());
        existingCompany.setPhone(dto.getPhone());
        existingCompany.setAddress(dto.getAddress());
        existingCompany.setPostalCode(dto.getPostalCode());
        existingCompany.setCity(dto.getCity());
        
        if (dto.getCountry() != null) {
            existingCompany.setCountry(dto.getCountry());
        }
        
        if (dto.getIsActive() != null) {
            existingCompany.setIsActive(dto.getIsActive());
        }
        
        // Salvar
        Company updatedCompany = companyRepository.save(existingCompany);
        
        // Retornar DTO
        return toResponseDTO(updatedCompany, false);
    }
    
    /**
     * Buscar empresa por ID
     */
    public CompanyResponseDTO getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        
        return toResponseDTO(company, false);
    }
    
    /**
     * Buscar empresa por ID com estatísticas (com verificação de permissão)
     */
    public CompanyResponseDTO getCompanyByIdWithStats(Long id, User currentUser) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        
        // Verificar permissão: ADMIN só pode ver stats da sua própria empresa
        if (currentUser.getRole() == Role.ADMIN) {
            if (currentUser.getCompany() == null || !currentUser.getCompany().getId().equals(id)) {
                throw new RuntimeException("You do not have permission to view statistics for this company");
            }
        }
        // SUPER_ADMIN pode ver stats de qualquer empresa
        
        return toResponseDTO(company, true);
    }
    
    /**
     * Buscar empresa por NIF
     */
    public CompanyResponseDTO getCompanyByNif(String nif) {
        Company company = companyRepository.findByNif(nif)
            .orElseThrow(() -> new RuntimeException("Company not found with NIF: " + nif));
        
        return toResponseDTO(company, false);
    }
    
    /**
     * Listar todas as empresas (filtragem baseada em role)
     */
    public List<CompanyResponseDTO> getAllCompanies(User currentUser) {
        List<Company> companies;
        
        // ADMIN vê apenas sua própria empresa
        if (currentUser.getRole() == Role.ADMIN) {
            if (currentUser.getCompany() != null) {
                companies = List.of(currentUser.getCompany());
            } else {
                companies = List.of();
            }
        } else {
            // SUPER_ADMIN e outros veem todas
            companies = companyRepository.findAll();
        }
        
        return companies.stream()
            .map(company -> toResponseDTO(company, false))
            .collect(Collectors.toList());
    }
    
    /**
     * Listar empresas ativas
     */
    public List<CompanyResponseDTO> getActiveCompanies() {
        return companyRepository.findByIsActiveTrue().stream()
            .map(company -> toResponseDTO(company, false))
            .collect(Collectors.toList());
    }
    
    /**
     * Listar empresas inativas (apenas SUPER_ADMIN)
     */
    public List<CompanyResponseDTO> getInactiveCompanies() {
        return companyRepository.findByIsActiveFalse().stream()
            .map(company -> toResponseDTO(company, false))
            .collect(Collectors.toList());
    }
    
    /**
     * Buscar empresas por nome (contendo)
     */
    public List<CompanyResponseDTO> searchCompaniesByName(String name) {
        return companyRepository.findByCompanyNameContainingIgnoreCase(name).stream()
            .map(company -> toResponseDTO(company, false))
            .collect(Collectors.toList());
    }
    
    /**
     * Buscar empresas por cidade
     */
    public List<CompanyResponseDTO> getCompaniesByCity(String city) {
        return companyRepository.findByCity(city).stream()
            .map(company -> toResponseDTO(company, false))
            .collect(Collectors.toList());
    }
    
    /**
     * Ativar empresa (apenas SUPER_ADMIN)
     */
    @Transactional
    public CompanyResponseDTO activateCompany(Long id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        
        company.setIsActive(true);
        Company updatedCompany = companyRepository.save(company);
        
        return toResponseDTO(updatedCompany, false);
    }
    
    /**
     * Desativar empresa (apenas SUPER_ADMIN)
     */
    @Transactional
    public CompanyResponseDTO deactivateCompany(Long id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        
        company.setIsActive(false);
        Company updatedCompany = companyRepository.save(company);
        
        return toResponseDTO(updatedCompany, false);
    }
    
    /**
     * Deletar empresa (apenas SUPER_ADMIN)
     */
    @Transactional
    public void deleteCompany(Long id) {
        Company company = companyRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
        
        // Verificar se tem produtos ou administradores
        if (company.getProducts() != null && !company.getProducts().isEmpty()) {
            throw new RuntimeException("Cannot delete company with associated products. Please remove products first.");
        }
        
        if (company.getAdministrators() != null && !company.getAdministrators().isEmpty()) {
            throw new RuntimeException("Cannot delete company with associated administrators. Please remove administrators first.");
        }
        
        // Deletar fisicamente
        companyRepository.delete(company);
    }
    
    /**
     * Deletar empresa forçadamente (apenas SUPER_ADMIN)
     */
    @Transactional
    public void hardDeleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            throw new RuntimeException("Company not found with id: " + id);
        }
        
        companyRepository.deleteById(id);
    }
    
    /**
     * Contar total de empresas
     */
    public long countAllCompanies() {
        return companyRepository.count();
    }
    
    /**
     * Contar empresas ativas
     */
    public long countActiveCompanies() {
        return companyRepository.countByIsActiveTrue();
    }
    
    /**
     * Método privado: Converte Company Entity para CompanyResponseDTO
     */
    private CompanyResponseDTO toResponseDTO(Company company, boolean includeStatistics) {
        CompanyResponseDTO dto = CompanyResponseDTO.builder()
            .id(company.getId())
            .companyName(company.getCompanyName())
            .tradeName(company.getTradeName())
            .nif(company.getNif())
            .email(company.getEmail())
            .phone(company.getPhone())
            .address(company.getAddress())
            .postalCode(company.getPostalCode())
            .city(company.getCity())
            .country(company.getCountry())
            .isActive(company.getIsActive())
            .build();
        
        // Incluir estatísticas se solicitado
        if (includeStatistics) {
            if (company.getProducts() != null) {
                dto.setTotalProducts((long) company.getProducts().size());
                dto.setActiveProducts(company.getProducts().stream()
                    .filter(p -> p.getIsActive() != null && p.getIsActive())
                    .count());
            }
            
            if (company.getAdministrators() != null) {
                dto.setTotalAdministrators(company.getAdministrators().stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .count());
            }
        }
        
        return dto;
    }
}