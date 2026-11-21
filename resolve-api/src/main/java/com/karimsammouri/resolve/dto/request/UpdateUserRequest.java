package com.karimsammouri.resolve.dto.request;

import com.karimsammouri.resolve.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1) String name,
        @Email String email,
        @Size(min = 8) String password,
        UserRole role,
        Boolean isEnabled
) {}