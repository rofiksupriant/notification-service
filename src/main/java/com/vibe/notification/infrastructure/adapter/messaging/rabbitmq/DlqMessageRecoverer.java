package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom message recoverer that adds error information to messages before sending to DLQ.
 * Adds custom headers:
 * - x-last-error: The error message
 * - x-last-error-timestamp: When the error occurred  
 * - x-original-queue: The original queue name
 */
public class DlqMessageRecoverer implements MessageRecoverer {

    private static final Logger logger = LoggerFactory.getLogger(DlqMessageRecoverer.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final String originalQueue;

    public DlqMessageRecoverer(RabbitTemplate rabbitTemplate, String exchange, String routingKey, String originalQueue) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.originalQueue = originalQueue;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        // Add custom headers
        MessageProperties properties = message.getMessageProperties();
        String errorMessage = cause != null ? cause.getMessage() : "Unknown error";
        properties.setHeader("x-last-error", errorMessage);
        properties.setHeader("x-last-error-timestamp", System.currentTimeMillis());
        properties.setHeader("x-original-queue", originalQueue);
        
        logger.warn("Sending message to DLQ after max retries. Error: {}", errorMessage);
        
        // Send to DLQ via the dead letter exchange
        rabbitTemplate.send(exchange, routingKey, message);
    }
}
