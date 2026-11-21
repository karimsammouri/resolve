package com.karimsammouri.resolve.dto.response;

import com.karimsammouri.resolve.enums.MessageSenderType;

import java.time.LocalDateTime;

public record ViewMessageResponse(
        Long messageId,
        MessageSenderType senderType,
        String senderName,
        String body,
        LocalDateTime createdAt
) {}
