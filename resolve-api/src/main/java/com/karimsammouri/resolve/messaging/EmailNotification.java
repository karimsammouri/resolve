package com.karimsammouri.resolve.messaging;

public record EmailNotification(
        EmailNotificationType type,
        String customerEmail,
        String customerName,
        Long ticketId,
        String ticketSubject,
        String accessLink
) {}
