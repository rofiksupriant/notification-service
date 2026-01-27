package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing processed message trace IDs.
 * Ensures idempotent message processing by tracking which messages have been processed
 * across both HTTP API and RabbitMQ message channels.
 */
@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
}

