package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByIdAndOrganization_Id(Long ticketId, Long organizationId);
}
