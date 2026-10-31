package com.karimsammouri.resolve.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentMessageRequest(
        @NotBlank String body,
        boolean isInternalNote
) {}
