package com.security.dto;

import java.util.Collection;
import java.util.Date;

import org.springframework.security.core.GrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Certifique-se de que essa anotação está presente
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

	  private Long id;
	    
	    private String userId;
	    private String fullName;
	    private String username;
	    private String password;
	    private String email;
	    private String profileImageUrl;
	    private Date lastLoginDate;
	    private Date lastLoginDateDisplay;
	    private Date joinDate;
	    private String role;
	    private boolean isActive = true;
	    private boolean isNotLocked = true;
	    private boolean isChangePassword = true;
	    private String languages;
	
	    
	
}