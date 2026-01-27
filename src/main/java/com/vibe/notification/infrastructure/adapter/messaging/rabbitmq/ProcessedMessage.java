package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing a processed notification request.
 * Stores trace IDs of successfully processed messages to prevent duplicate handling
 * across both HTTP API and RabbitMQ message channels.
 */
@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {
    @Id
    private String traceId;

    public ProcessedMessage() {
    }

    public ProcessedMessage(String traceId) {
        this.traceId = traceId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
