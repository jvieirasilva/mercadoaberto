package com.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.security.model.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    // ✅ Buscar item específico no carrinho
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    Optional<CartItem> findByCartIdAndProductId(
        @Param("cartId") Long cartId, 
        @Param("productId") Long productId
    );
    
    // ✅ Verificar se produto já está no carrinho
    boolean existsByCartIdAndProductId(Long cartId, Long productId);
    
    // ✅ Deletar todos os itens de um carrinho
    void deleteByCartId(Long cartId);
}