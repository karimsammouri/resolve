package com.karimsammouri.resolve.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddMessageRequest(
        @NotBlank String body
) {}
