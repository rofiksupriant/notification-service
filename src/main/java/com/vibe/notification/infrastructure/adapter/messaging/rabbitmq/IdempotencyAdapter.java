package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import com.vibe.notification.application.port.IdempotencyPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Idempotency adapter implementing IdempotencyPort
 * Ensures idempotent message processing by tracking which messages have been processed
 */
@Component
public class IdempotencyAdapter implements IdempotencyPort {
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyAdapter.class);

    private final ProcessedMessageRepository processedMessageRepository;

    public IdempotencyAdapter(ProcessedMessageRepository processedMessageRepository) {
        this.processedMessageRepository = processedMessageRepository;
    }

    /**
     * Check if a message with the given ID has been processed
     */
    @Override
    public boolean isMessageAlreadyProcessed(String messageId) {
        boolean exists = processedMessageRepository.existsById(messageId);
        logger.debug("Checking if message {} has been processed: {}", messageId, exists);
        return exists;
    }

    /**
     * Mark a message as processed
     */
    @Override
    public void markMessageAsProcessed(String messageId) {
        try {
            ProcessedMessage processedMessage = new ProcessedMessage(messageId);
            processedMessageRepository.save(processedMessage);
            logger.info("Message marked as processed: {}", messageId);
        } catch (Exception e) {
            logger.error("Failed to mark message as processed: {}", messageId, e);
            throw new RuntimeException("Failed to mark message as processed: " + e.getMessage(), e);
        }
    }
}
