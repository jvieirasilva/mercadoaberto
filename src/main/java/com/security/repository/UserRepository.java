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
	  /**
     * Buscar por email (sem carregar company)
     */
    Optional<User> findByEmail(String email);
    
    /**
     * ✅ NOVO - Buscar por email COM company carregada (JOIN FETCH)
     * Use este método para autenticação!
     */
    @Query("""
    		  select u from User u
    		  left join fetch u.company
    		  where u.email = :email
    		""")
    		Optional<User> findByEmailWithCompany(@Param("email") String email);
    
    
    /**
     * ✅ NOVO - Buscar por ID COM company carregada
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company WHERE u.id = :id")
    Optional<User> findByIdWithCompany(@Param("id") Long id);
    
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

