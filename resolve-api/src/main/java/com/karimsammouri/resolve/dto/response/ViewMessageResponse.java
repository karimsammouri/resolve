package com.karimsammouri.resolve.dto.response;

import java.time.LocalDateTime;

public record ViewMessageResponse(
        Long messageId,
        String senderType,
        String senderName,
        String body,
        LocalDateTime createdAt
) {}
