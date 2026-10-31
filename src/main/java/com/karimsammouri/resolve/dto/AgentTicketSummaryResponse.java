package com.karimsammouri.resolve.dto;

import java.time.LocalDateTime;

public record AgentTicketSummaryResponse(
        Long ticketId,
        String subject,
        String status,
        String customerName,
        String customerEmail,
        String assignedAgentName,
        boolean hasUnreadCustomerMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
