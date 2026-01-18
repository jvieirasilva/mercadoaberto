package com.security.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {

    private Long id;
    private String name;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    private BigDecimal price;
    private BigDecimal stockQuantity;
    private String description;
    private Boolean isActive;
    
    private Long companyId;
}
