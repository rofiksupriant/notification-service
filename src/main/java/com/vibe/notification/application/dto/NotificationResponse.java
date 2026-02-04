package com.vibe.notification.application.dto;

import com.vibe.notification.domain.model.NotificationStatus;

import java.util.UUID;

/**
 * Notification Response DTO
 * 
 * @param logId the internal notification log ID (UUID)
 * @param traceId the trace ID as string (can be client-provided or server-generated UUID converted to string)
 * @param status the current status of the notification ("PENDING", "ALREADY_PROCESSED", "ACCEPTED", "SUCCESS", "FAILED")
 * @param message a descriptive message about the notification processing
 * @param providerStatus optional provider status (only populated in sync mode)
 */
public record NotificationResponse(
    UUID logId,
    String traceId,
    String status,
    String message,
    NotificationStatus providerStatus
) {
    /**
     * Constructor for async responses (no provider status)
     */
    public NotificationResponse(UUID logId, String traceId, String status, String message) {
        this(logId, traceId, status, message, null);
    }
}
