package com.vibe.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_logs_trace_id", columnList = "trace_id"),
    @Index(name = "idx_logs_recipient", columnList = "recipient"),
    @Index(name = "idx_logs_status", columnList = "status"),
    @Index(name = "idx_logs_created_at", columnList = "created_at")
})
public class NotificationLogEntity {
    
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "trace_id", nullable = false)
    private UUID traceId;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "variables", columnDefinition = "JSONB")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode variables;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public NotificationLogEntity() {}

    public NotificationLogEntity(UUID id, UUID traceId, String recipient, String slug,
                               String channel, JsonNode variables, String status) {
        this.id = id;
        this.traceId = traceId;
        this.recipient = recipient;
        this.slug = slug;
        this.channel = channel;
        this.variables = variables;
        this.status = status;
    }

    // Getters & Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTraceId() {
        return traceId;
    }

    public void setTraceId(UUID traceId) {
        this.traceId = traceId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public JsonNode getVariables() {
        return variables;
    }

    public void setVariables(JsonNode variables) {
        this.variables = variables;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
