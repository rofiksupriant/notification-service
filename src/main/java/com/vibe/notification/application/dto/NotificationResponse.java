package com.vibe.notification.application.dto;

import java.util.UUID;

/**
 * Notification Response DTO
 * 
 * @param logId the internal notification log ID (UUID)
 * @param traceId the trace ID as string (can be client-provided or server-generated UUID converted to string)
 * @param status the current status of the notification ("PENDING", "ALREADY_PROCESSED", etc.)
 * @param message a descriptive message about the notification processing
 */
public record NotificationResponse(
    UUID logId,
    String traceId,
    String status,
    String message
) {}
