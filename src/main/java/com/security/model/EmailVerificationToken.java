package com.security.model;

import java.util.Date;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_verification_token")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EmailVerificationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 500)
    private String token;
    
    @Column(nullable = false, length = 255)
    private String email;
    
    @Column(nullable = false)
    private Date createdDate;
    
    @Column(nullable = false)
    private Date expiryDate;
    
    @Column(nullable = false)
    private boolean used = false;
  
}