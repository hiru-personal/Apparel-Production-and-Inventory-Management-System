package org.example.pim_system.repository;

import org.example.pim_system.model.ProductProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductProgressRepository extends JpaRepository<ProductProgress, Long> {

    Optional<ProductProgress> findByProductCode(String productCode);
}

