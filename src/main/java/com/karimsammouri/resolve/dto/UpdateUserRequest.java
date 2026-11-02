package com.karimsammouri.resolve.dto;

import com.karimsammouri.resolve.enums.UserRole;

public record UpdateUserRequest(
        String name,
        String email,
        String password,
        UserRole role,
        Boolean isEnabled
) {}