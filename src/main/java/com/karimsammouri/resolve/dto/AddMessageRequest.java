package com.karimsammouri.resolve.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMessageRequest(
        @NotBlank String body
) {}
