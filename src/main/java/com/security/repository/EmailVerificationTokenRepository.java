package com.security.repository;

import java.util.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.security.model.EmailVerificationToken;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    /**
     * Buscar token por string do token
     * 
     * @param token String UUID do token
     * @return Optional com o token encontrado
     */
    Optional<EmailVerificationToken> findByToken(String token);
    
    /**
     * Buscar token por email do usuário
     * 
     * @param email Email do usuário
     * @return Optional com o token encontrado
     */
    Optional<EmailVerificationToken> findByEmail(String email);
    
    /**
     * Deletar todos os tokens de um email
     * Útil para limpar tokens antigos antes de criar um novo
     * 
     * @param email Email do usuário
     */
    void deleteByEmail(String email);
    
    /**
     * Deletar tokens expirados
     * Útil para job de limpeza automática
     * 
     * @param date Data de corte (tokens com expiryDate anterior a esta data serão deletados)
     */
    void deleteByExpiryDateBefore(Date date);
    
    /**
     * Verificar se existe token válido (não usado e não expirado) para um email
     * 
     * @param email Email do usuário
     * @param currentDate Data atual para comparar com expiryDate
     * @return true se existe token válido
     */
    boolean existsByEmailAndUsedFalseAndExpiryDateAfter(String email, Date currentDate);
    
    /**
     * Buscar todos os tokens não usados de um email
     * 
     * @param email Email do usuário
     * @return Lista de tokens não usados
     */
    java.util.List<EmailVerificationToken> findByEmailAndUsedFalse(String email);
}