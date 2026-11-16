package com.karimsammouri.emailservice.messaging;

public record EmailNotification(
        EmailNotificationType type,
        String customerEmail,
        String customerName,
        Long ticketId,
        String ticketSubject,
        String accessLink
) {}
