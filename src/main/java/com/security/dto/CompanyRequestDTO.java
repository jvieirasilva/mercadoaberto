package com.security.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyRequestDTO {
    
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name max length is 255")
    private String companyName;
    
    @Size(max = 255, message = "Trade name max length is 255")
    private String tradeName;
    
    @NotBlank(message = "NIF is required")
    @Pattern(regexp = "^[0-9]{9}$", message = "NIF must have exactly 9 digits")
    private String nif;
    
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email max length is 255")
    private String email;
    
    @Pattern(regexp = "^(\\+351)?[0-9]{9}$", message = "Invalid phone number format")
    @Size(max = 20, message = "Phone max length is 20")
    private String phone;
    
    @Size(max = 500, message = "Address max length is 500")
    private String address;
    
    @Pattern(regexp = "^[0-9]{4}-[0-9]{3}$", message = "Postal code must be in format XXXX-XXX")
    private String postalCode;
    
    @Size(max = 100, message = "City max length is 100")
    private String city;
    
    @Size(max = 100, message = "Country max length is 100")
    private String country = "Portugal";
    
    private Boolean isActive = true;
}