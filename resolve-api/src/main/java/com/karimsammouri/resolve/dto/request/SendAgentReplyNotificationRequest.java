package com.karimsammouri.resolve.dto.request;

public record SendAgentReplyNotificationRequest(
        String customerEmail,
        String customerName,
        Long ticketId,
        String ticketSubject
) {}
