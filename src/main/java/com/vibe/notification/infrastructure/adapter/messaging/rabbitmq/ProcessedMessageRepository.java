package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Repository for managing processed message trace IDs.
 * Ensures idempotent message processing by tracking which messages have been processed.
 */
@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
}

/**
 * Entity representing a processed RabbitMQ message.
 * Stores trace IDs of successfully processed messages to prevent duplicate handling.
 */
@Entity
@Table(name = "processed_messages")
class ProcessedMessage {
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
