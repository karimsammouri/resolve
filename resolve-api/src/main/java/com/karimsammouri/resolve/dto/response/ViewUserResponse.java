package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.UserRole;

import java.time.LocalDateTime;

public record ViewUserResponse(
        Long userId,
        String name,
        String email,
        UserRole role,
        boolean isEnabled,
        LocalDateTime createdAt
) {}
