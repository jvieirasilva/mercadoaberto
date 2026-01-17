package com.security.repository;

import java.util.Date;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.security.model.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByToken(String token);
    
    Optional<PasswordResetToken> findByEmail(String email);
    
    void deleteByEmail(String email);
    
    void deleteByExpiryDateBefore(Date date);
    
    boolean existsByEmailAndUsedFalseAndExpiryDateAfter(String email, Date currentDate);
}