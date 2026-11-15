package com.karimsammouri.resolve.dto.response;

import java.util.List;

public record ViewTicketResponse(
        Long ticketId,
        String subject,
        String status,
        String customerName,
        String customerEmail,
        List<ViewMessageResponse> messages
) {}