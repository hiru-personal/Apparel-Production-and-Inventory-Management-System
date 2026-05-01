package org.example.pim_system.repository;

import org.example.pim_system.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    List<SupportTicket> findAllByOrderByCreatedAtDesc();
}

