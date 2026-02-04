package com.vibe.notification.domain.model;

import java.time.LocalDateTime;

/**
 * Domain event representing a notification status update.
 * This event is published to RabbitMQ to notify the Main App about notification outcomes.
 * 
 * @param traceId Original correlation ID (trace_id)
 * @param status Notification outcome (SUCCESS, FAILED, RETRY_EXHAUSTED)
 * @param channel The channel used (EMAIL/WHATSAPP)
 * @param errorMessage Root cause of failure (null if success)
 * @param timestamp Event occurrence time
 * @param clientId Optional client identifier for routing to client-specific queues
 */
public record NotificationStatusEvent(
    String traceId,
    NotificationStatus status,
    Channel channel,
    String errorMessage,
    LocalDateTime timestamp,
    String clientId
) {
    /**
     * Create a success event
     */
    public static NotificationStatusEvent success(String traceId, Channel channel, String clientId) {
        return new NotificationStatusEvent(
            traceId,
            NotificationStatus.SUCCESS,
            channel,
            null,
            LocalDateTime.now(),
            clientId
        );
    }
    
    /**
     * Create a failed event with error message
     */
    public static NotificationStatusEvent failure(String traceId, Channel channel, String errorMessage, String clientId) {
        return new NotificationStatusEvent(
            traceId,
            NotificationStatus.FAILED,
            channel,
            errorMessage,
            LocalDateTime.now(),
            clientId
        );
    }
    
    /**
     * Create a retry exhausted event with error message
     */
    public static NotificationStatusEvent retryExhausted(String traceId, Channel channel, String errorMessage, String clientId) {
        return new NotificationStatusEvent(
            traceId,
            NotificationStatus.RETRY_EXHAUSTED,
            channel,
            errorMessage,
            LocalDateTime.now(),
            clientId
        );
    }
}
