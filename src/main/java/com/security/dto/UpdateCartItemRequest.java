package com.security.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ? Request para atualizar quantidade de item
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}