package com.karimsammouri.resolve.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AgentTicketResponse(
        Long ticketId,
        String subject,
        String status,
        String customerName,
        String customerEmail,
        String assignedAgentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AgentMessageResponse> messages
) {}