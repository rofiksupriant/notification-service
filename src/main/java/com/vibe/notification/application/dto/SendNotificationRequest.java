package com.vibe.notification.application.dto;

import com.vibe.notification.domain.model.Channel;
import java.util.Map;
import java.util.Optional;

/**
 * Send Notification Request DTO
 * 
 * @param recipient the notification recipient (phone number for WhatsApp, email for Email)
 * @param slug the template identifier
 * @param language the ISO 639-1 language code
 * @param channel the notification channel (EMAIL or WHATSAPP)
 * @param variables template variables for interpolation
 * @param traceId optional idempotency key - if provided, ensures request is processed only once
 * @param clientId optional client identifier for routing status callbacks to client-specific queues
 */
public record SendNotificationRequest(
    String recipient,
    String slug,
    String language,
    Channel channel,
    Map<String, Object> variables,
    Optional<String> traceId,
    Optional<String> clientId
) {
    /**
     * Convenience constructor without traceId and clientId (generates new trace ID internally)
     */
    public SendNotificationRequest(
        String recipient,
        String slug,
        String language,
        Channel channel,
        Map<String, Object> variables
    ) {
        this(recipient, slug, language, channel, variables, Optional.empty(), Optional.empty());
    }
    
    /**
     * Convenience constructor with traceId only (no clientId)
     */
    public SendNotificationRequest(
        String recipient,
        String slug,
        String language,
        Channel channel,
        Map<String, Object> variables,
        Optional<String> traceId
    ) {
        this(recipient, slug, language, channel, variables, traceId, Optional.empty());
    }
}

