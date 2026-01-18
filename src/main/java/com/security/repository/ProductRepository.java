package com.security.repository;

import com.security.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p
          FROM Product p
         WHERE (:term IS NULL OR :term = '' 
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :term, '%'))
         )
        """)
    Page<Product> searchByNameOrDescription(@Param("term") String term, Pageable pageable);
    
    @Query("""
    		   SELECT p
    		     FROM Product p
    		    WHERE p.company.id = :companyId
    		      AND (
    		           :term IS NULL OR :term = ''
    		           OR LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
    		           OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :term, '%'))
    		      )
    		""")
    		Page<Product> searchByCompanyId(
    		    @Param("companyId") Long companyId,
    		    @Param("term") String term,
    		    Pageable pageable
    		);
}
