package com.karimsammouri.emailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SendAgentReplyNotificationRequest(
        @NotBlank @Email String customerEmail,
        @NotBlank String customerName,
        @NotNull @Positive Long ticketId,
        @NotBlank String ticketSubject
) {}
