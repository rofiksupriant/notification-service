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
 */
public record SendNotificationRequest(
    String recipient,
    String slug,
    String language,
    Channel channel,
    Map<String, Object> variables,
    Optional<String> traceId
) {
    /**
     * Convenience constructor without traceId (generates new one internally)
     */
    public SendNotificationRequest(
        String recipient,
        String slug,
        String language,
        Channel channel,
        Map<String, Object> variables
    ) {
        this(recipient, slug, language, channel, variables, Optional.empty());
    }
}

