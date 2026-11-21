package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.TicketStatus;

import java.time.LocalDateTime;

public record CreateTicketResponse(
        Long ticketId,
        String subject,
        TicketStatus status,
        LocalDateTime createdAt
) {}
