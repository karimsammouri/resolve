package com.karimsammouri.resolve.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String customerName,
        @NotBlank @Email String customerEmail,
        @NotBlank String messageSubject,
        @NotBlank String messageBody
) {}
