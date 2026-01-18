package com.security.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Company")
public class Company {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Company name is required")
    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;
    
    @Column(name = "trade_name", length = 255)
    private String tradeName;
    
    @NotBlank(message = "NIF is required")
    @Pattern(regexp = "^[0-9]{9}$", message = "NIF must have exactly 9 digits")
    @Column(nullable = false, unique = true, length = 9)
    private String nif;
    
    @Email(message = "Invalid email format")
    @Column(length = 255)
    private String email;
    
    @Pattern(regexp = "^(\\+351)?[0-9]{9}$", message = "Invalid phone number format")
    @Column(length = 20)
    private String phone;
    
    @Column(length = 500)
    private String address;
    
    @Pattern(regexp = "^[0-9]{4}-[0-9]{3}$", message = "Postal code must be in format XXXX-XXX")
    @Column(name = "postal_code", length = 8)
    private String postalCode;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String country = "Portugal";
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    // Relacionamento com Products
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();
    
    // Relacionamento com Users (ADMIN)
    @OneToMany(mappedBy = "company")
    private List<User> administrators = new ArrayList<>();
}