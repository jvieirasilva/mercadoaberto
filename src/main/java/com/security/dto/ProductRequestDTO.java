package com.security.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @Builder.Default
    private List<@Size(max = 500, message = "Image URL max length is 500") String> images = new ArrayList<>();

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @DecimalMin(value = "0.00", message = "Stock quantity cannot be negative")
    private BigDecimal stockQuantity;

    @Size(max = 1000, message = "Description max length is 1000")
    private String description;

    private Boolean isActive = true;
    
    @Builder.Default
    private List<MultipartFile> productImages = new ArrayList<>();
    
    @NotNull(message = "companyId  is required")
    private Long companyId;
}
