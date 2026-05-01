package org.example.pim_system.repository;

import org.example.pim_system.model.MissingNeedle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MissingNeedleRepository extends JpaRepository<MissingNeedle, Long> {
    List<MissingNeedle> findAllByOrderByReportedAtDesc();
    List<MissingNeedle> findByStatus(String status);
}



