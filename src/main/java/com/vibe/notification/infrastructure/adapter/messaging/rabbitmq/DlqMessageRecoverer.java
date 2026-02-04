package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.domain.model.NotificationStatusEvent;
import com.vibe.notification.domain.port.NotificationStatusProducer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom message recoverer that adds error information to messages before sending to DLQ.
 * Also publishes RETRY_EXHAUSTED status event when a message fails all retries.
 * 
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
    private final NotificationStatusProducer notificationStatusProducer;
    private final ObjectMapper objectMapper;

    public DlqMessageRecoverer(
            RabbitTemplate rabbitTemplate, 
            String exchange, 
            String routingKey, 
            String originalQueue,
            NotificationStatusProducer notificationStatusProducer,
            ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.originalQueue = originalQueue;
        this.notificationStatusProducer = notificationStatusProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        // Add custom headers
        MessageProperties properties = message.getMessageProperties();
        String errorMessage = (cause != null && cause.getMessage() != null) 
            ? cause.getMessage() 
            : "Unknown error";
        properties.setHeader("x-last-error", errorMessage);
        properties.setHeader("x-last-error-timestamp", System.currentTimeMillis());
        properties.setHeader("x-original-queue", originalQueue);
        
        logger.warn("Sending message to DLQ after max retries. Error: {}", errorMessage);
        
        // Extract trace_id and channel from the message to publish RETRY_EXHAUSTED status
        try {
            String messageBody = new String(message.getBody(), "UTF-8");
            NotificationRequestMessage requestMessage = objectMapper.readValue(messageBody, NotificationRequestMessage.class);
            
            // Publish RETRY_EXHAUSTED status
            NotificationStatusEvent event = NotificationStatusEvent.retryExhausted(
                requestMessage.traceId(),
                requestMessage.channel(),
                errorMessage
            );
            
            notificationStatusProducer.publishStatus(event);
            logger.info("Published RETRY_EXHAUSTED status for trace_id={}", requestMessage.traceId());
            
        } catch (Exception e) {
            // Log but don't fail - DLQ recovery is more important
            logger.error("Failed to publish RETRY_EXHAUSTED status: {}", e.getMessage(), e);
        }
        
        // Send to DLQ via the dead letter exchange
        rabbitTemplate.send(exchange, routingKey, message);
    }
}
