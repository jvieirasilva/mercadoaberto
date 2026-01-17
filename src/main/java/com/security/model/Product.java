package com.security.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Product")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Name is required")
    @Column(nullable = false, length = 255)
    private String name;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "product_images", 
        joinColumns = @JoinColumn(name = "product_id")
    )
    @Column(name = "image_url", length = 500)
    private List<String> images = new ArrayList<>();
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @NotNull(message = "Stock quantity is required")
    @DecimalMin(value = "0.00", message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal stockQuantity;

    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}