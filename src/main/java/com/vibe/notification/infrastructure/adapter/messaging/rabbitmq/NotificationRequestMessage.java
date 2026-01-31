package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import com.vibe.notification.domain.model.Channel;
import java.util.Map;

/**
 * Message payload for notification requests received via RabbitMQ.
 * Ensures decoupling between the main application and notification service.
 * Structure mirrors SendNotificationRequest for consistency across HTTP and messaging APIs.
 *
 * @param traceId unique identifier for idempotent processing (prevents duplicate handling)
 * @param recipient the message recipient (phone number for WhatsApp, email address for Email)
 * @param slug the unique identifier of the template to use for rendering
 * @param language the ISO 639-1 language code (e.g., "en", "id", "es")
 * @param channel the notification channel (EMAIL or WHATSAPP)
 * @param variables a map of template variables to be interpolated into the template content
 */
public record NotificationRequestMessage(
    String traceId,
    String recipient,
    String slug,
    String language,
    Channel channel,
    Map<String, Object> variables
) {
}
