package com.security.dto;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String fullName;
    private String email;
    private String password;
    private String role;
     
    
    @JsonProperty("isActive")
    private Boolean isActive; // Boolean, não boolean
    
    @JsonProperty("isNotLocked")
    private Boolean isNotLocked; // Boolean, não boolean
    
    @JsonProperty("isChangePassword")
    private Boolean isChangePassword; // Boolean, não boolean
    
    
    
    private MultipartFile profileImage;
}