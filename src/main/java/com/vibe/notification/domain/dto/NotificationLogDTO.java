package com.vibe.notification.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Data Transfer Object for notification logs
 * Used to transfer log data without exposing infrastructure entity details
 */
public class NotificationLogDTO {
    private final UUID id;
    private final UUID traceId;
    private final String slug;
    private final String language;
    private final String channel;
    private final String recipient;
    private final JsonNode variables;
    private final String status;
    private final String errorMessage;
    private final LocalDateTime sentAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public NotificationLogDTO(
            UUID id,
            UUID traceId,
            String slug,
            String language,
            String channel,
            String recipient,
            JsonNode variables,
            String status,
            String errorMessage,
            LocalDateTime sentAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.traceId = traceId;
        this.slug = slug;
        this.language = language;
        this.channel = channel;
        this.recipient = recipient;
        this.variables = variables;
        this.status = status;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTraceId() {
        return traceId;
    }

    public String getSlug() {
        return slug;
    }

    public String getLanguage() {
        return language;
    }

    public String getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public JsonNode getVariables() {
        return variables;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
