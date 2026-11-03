package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.UserRole;

public record AuthResponse(
        String name,
        String email,
        UserRole role
) {}
