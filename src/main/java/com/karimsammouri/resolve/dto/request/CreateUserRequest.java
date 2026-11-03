package com.karimsammouri.resolve.dto.request;

import com.karimsammouri.resolve.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotBlank String name,
        @NotBlank String email,
        @NotBlank String password,
        @NotNull UserRole role
) {}
