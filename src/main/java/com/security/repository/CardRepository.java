package com.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.security.model.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>,
JpaSpecificationExecutor<Card>
{

}
