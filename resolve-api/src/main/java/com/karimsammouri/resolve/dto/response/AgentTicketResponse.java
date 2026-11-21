package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AgentTicketResponse(
        Long ticketId,
        Long organizationId,
        String subject,
        TicketStatus status,
        String customerName,
        String customerEmail,
        String assignedAgentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AgentMessageResponse> messages
) {}