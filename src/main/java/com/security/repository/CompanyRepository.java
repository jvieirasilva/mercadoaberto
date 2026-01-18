package com.security.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.security.model.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    
    /**
     * Buscar empresa por NIF
     */
    Optional<Company> findByNif(String nif);
    
    /**
     * Verificar se existe empresa com determinado NIF
     */
    boolean existsByNif(String nif);
    
    /**
     * Verificar se existe empresa com NIF, excluindo um ID específico (útil para update)
     */
    boolean existsByNifAndIdNot(String nif, Long id);
    
    /**
     * Buscar empresas ativas
     */
    List<Company> findByIsActiveTrue();
    
    /**
     * Buscar empresas inativas
     */
    List<Company> findByIsActiveFalse();
    
    /**
     * Buscar empresas por nome (contendo, ignorando case)
     */
    List<Company> findByCompanyNameContainingIgnoreCase(String companyName);
    
    /**
     * Buscar empresas por cidade
     */
    List<Company> findByCity(String city);
    
    /**
     * Buscar empresas ativas por cidade
     */
    List<Company> findByCityAndIsActiveTrue(String city);
    
    /**
     * Buscar empresa por email
     */
    Optional<Company> findByEmail(String email);
    
    /**
     * Contar empresas ativas
     */
    long countByIsActiveTrue();
    
    /**
     * Query customizada: Buscar empresas com produtos
     */
    @Query("SELECT DISTINCT c FROM Company c LEFT JOIN FETCH c.products WHERE c.id = :id")
    Optional<Company> findByIdWithProducts(@Param("id") Long id);
    
    /**
     * Query customizada: Buscar empresas com administradores
     */
    @Query("SELECT DISTINCT c FROM Company c LEFT JOIN FETCH c.administrators WHERE c.id = :id")
    Optional<Company> findByIdWithAdministrators(@Param("id") Long id);
}