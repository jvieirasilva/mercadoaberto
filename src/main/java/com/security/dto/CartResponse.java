package com.security.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ? Response do carrinho completo
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    
    private Long cartId;
    private Long userId;
    private String userEmail;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}