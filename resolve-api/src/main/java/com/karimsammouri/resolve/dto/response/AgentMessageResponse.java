package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.MessageSenderType;

import java.time.LocalDateTime;

public record AgentMessageResponse(
        Long messageId,
        MessageSenderType senderType,
        String senderName,
        String body,
        boolean isInternalNote,
        LocalDateTime createdAt
) {}