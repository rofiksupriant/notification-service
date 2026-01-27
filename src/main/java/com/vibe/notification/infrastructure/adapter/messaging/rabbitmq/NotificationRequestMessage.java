package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import java.util.Map;

/**
 * Message payload for notification requests received via RabbitMQ.
 * Ensures decoupling between the main application and notification service.
 *
 * @param traceId unique identifier for idempotent processing (prevents duplicate handling)
 * @param recipient the message recipient (phone number for WhatsApp, email address for Email)
 * @param templateSlug the unique identifier of the template to use for rendering
 * @param language the ISO 639-1 language code (e.g., "en", "id", "es")
 * @param variables a map of template variables to be interpolated into the template content
 */
public record NotificationRequestMessage(
    String traceId,
    String recipient,
    String templateSlug,
    String language,
    Map<String, Object> variables
) {
}
