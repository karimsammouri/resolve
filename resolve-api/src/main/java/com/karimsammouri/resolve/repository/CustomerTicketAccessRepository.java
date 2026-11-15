package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.CustomerTicketAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerTicketAccessRepository extends JpaRepository<CustomerTicketAccess, Long> {
    Optional<CustomerTicketAccess> findByTicket_IdAndTokenHashAndRevokedAtIsNull(Long ticketId, String tokenHash);
}
