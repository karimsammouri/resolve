package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.TicketStatus;

import java.util.List;

public record ViewTicketResponse(
        Long ticketId,
        String subject,
        TicketStatus status,
        String customerName,
        String customerEmail,
        List<ViewMessageResponse> messages
) {}