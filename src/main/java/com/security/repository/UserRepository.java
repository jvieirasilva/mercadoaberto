package com.security.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.security.model.User;



@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    /**
     * Pesquisa usuários por nome ou email (case-insensitive)
     */
    @Query("""
            SELECT u FROM User u
            WHERE (
                LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            )
            AND u.isActive = true
            AND u.isNotLocked = true
        """)
        Page<User> searchByNameOrEmail(@Param("searchTerm") String searchTerm, Pageable pageable);
    }