package com.security.model;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_item")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    private Integer quantity;
    
    // ✅ IMPORTANTE: Salvar preço no momento da adição
    // Caso o preço do produto mude depois, o carrinho mantém o preço original
    private BigDecimal priceAtAddition;
    
    // ✅ Calcular subtotal
    public BigDecimal getSubtotal() {
        if (priceAtAddition == null || quantity == null) {
            return BigDecimal.ZERO;
        }
        return priceAtAddition.multiply(BigDecimal.valueOf(quantity));
    }
}