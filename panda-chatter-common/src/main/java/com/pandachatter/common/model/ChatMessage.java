package com.pandachatter.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Shared chat message model — serialized as JSON over Kafka.
 */
public record ChatMessage(
        String id,
        String username,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp
) {}
