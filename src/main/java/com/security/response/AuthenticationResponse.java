package com.security.response;



import com.security.dto.UserDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder 
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
       
    
}