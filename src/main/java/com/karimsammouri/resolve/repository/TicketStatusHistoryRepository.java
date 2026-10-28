package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.TicketStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {
}
