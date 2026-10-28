package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
