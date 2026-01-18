package com.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponseDTO {
    
    private Long id;
    private String companyName;
    private String tradeName;
    private String nif;
    private String email;
    private String phone;
    private String address;
    private String postalCode;
    private String city;
    private String country;
    private Boolean isActive;
    
    // Estatísticas opcionais
    private Long totalProducts;
    private Long activeProducts;
    private Long totalAdministrators;
}