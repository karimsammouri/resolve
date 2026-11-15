package com.karimsammouri.resolve.dto.request;

public record SendTicketConfirmationRequest(
        String customerEmail,
        String customerName,
        Long ticketId,
        String ticketSubject,
        String accessLink
) {}
