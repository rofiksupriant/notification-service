package com.vibe.notification.application.dto;

import java.util.UUID;

/**
 * Notification Response DTO
 */
public record NotificationResponse(
    UUID logId,
    UUID traceId,
    String status,
    String message
) {}
