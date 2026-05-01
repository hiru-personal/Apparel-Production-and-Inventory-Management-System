package org.example.pim_system.repository;

import org.example.pim_system.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByProductCodeAsc();

    boolean existsByProductCode(String productCode);

    Optional<Product> findByProductCode(String productCode);
}
