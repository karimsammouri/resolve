package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.TicketStatus;

import java.time.LocalDateTime;

public record AgentTicketSummaryResponse(
        Long ticketId,
        String subject,
        TicketStatus status,
        String customerName,
        String customerEmail,
        String assignedAgentName,
        boolean hasUnreadCustomerMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
