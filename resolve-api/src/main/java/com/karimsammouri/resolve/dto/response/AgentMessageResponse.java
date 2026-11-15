package com.karimsammouri.resolve.dto.response;

import java.time.LocalDateTime;

public record AgentMessageResponse(
        Long messageId,
        String senderType,
        String senderName,
        String body,
        boolean isInternalNote,
        LocalDateTime createdAt
) {}