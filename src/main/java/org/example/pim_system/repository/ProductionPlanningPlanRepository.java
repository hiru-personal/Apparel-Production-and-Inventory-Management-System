package org.example.pim_system.repository;

import org.example.pim_system.model.ProductionPlanningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionPlanningPlanRepository extends JpaRepository<ProductionPlanningPlan, Long> {

    List<ProductionPlanningPlan> findAllByOrderByCreatedAtDesc();

    List<ProductionPlanningPlan> findAllByProductCodeOrderByCreatedAtDesc(String productCode);
}

