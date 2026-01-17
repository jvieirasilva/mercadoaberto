package com.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.security.model.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    // ✅ Buscar carrinho por ID do usuário
    Optional<Cart> findByUserId(Long userId);
    
    // ✅ Buscar carrinho por email do usuário
    @Query("SELECT c FROM Cart c WHERE c.user.email = :email")
    Optional<Cart> findByUserEmail(@Param("email") String email);
    
    // ✅ Verificar se usuário já tem carrinho
    boolean existsByUserId(Long userId);
    
    // ✅ Deletar carrinho por ID do usuário
    void deleteByUserId(Long userId);
}