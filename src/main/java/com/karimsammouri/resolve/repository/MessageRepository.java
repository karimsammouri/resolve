package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByTicket_IdAndIsInternalNoteFalseOrderByCreatedAtAsc(Long ticketId);
    List<Message> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
