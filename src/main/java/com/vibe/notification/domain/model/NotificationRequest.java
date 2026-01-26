package com.vibe.notification.domain.model;

import java.util.Map;

/**
 * Notification Request - Java 25 Record for immutable data transfer
 */
public record NotificationRequest(
    String recipient,
    String slug,
    String language,
    Channel channel,
    Map<String, Object> variables
) {
    public NotificationRequest {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient cannot be null or blank");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Slug cannot be null or blank");
        }
        if (channel == null) {
            throw new IllegalArgumentException("Channel cannot be null");
        }
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("Language cannot be null or blank");
        }
    }
}
