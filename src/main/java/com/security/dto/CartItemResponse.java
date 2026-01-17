package com.security.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ✅ Response de item do carrinho
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productDescription;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal priceAtAddition;
    private BigDecimal subtotal;
    private BigDecimal maxStock; // ✅ MUDADO PARA BigDecimal (estoque é BigDecimal no Product)
}