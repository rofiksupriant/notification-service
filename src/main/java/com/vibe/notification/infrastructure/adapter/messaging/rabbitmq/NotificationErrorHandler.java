package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Custom error handler for RabbitMQ listener.
 * Differentiates between transient errors (retryable) and client errors (non-retryable).
 * Adds custom header 'x-last-error' when messages are sent to DLQ.
 */
@Component
public class NotificationErrorHandler implements RabbitListenerErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationErrorHandler.class);
    
    @Override
    public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message,
                            ListenerExecutionFailedException exception) {
        
        Throwable cause = exception.getCause();
        String errorMessage = cause != null ? cause.getMessage() : exception.getMessage();
        
        logger.error("Error processing message: {}", errorMessage, exception);
        
        // Add custom header with error information
        MessageProperties properties = amqpMessage.getMessageProperties();
        properties.setHeader("x-last-error", errorMessage);
        properties.setHeader("x-last-error-timestamp", System.currentTimeMillis());
        
        // Determine if error is retryable
        if (isClientError(cause)) {
            // Client errors (4xx) are non-retryable - reject and don't requeue
            logger.warn("Client error detected (4xx), sending to DLQ: {}", errorMessage);
            throw new AmqpRejectAndDontRequeueException("Client error - not retrying: " + errorMessage, exception);
        }
        
        // Transient errors (5xx, timeout, etc.) will be retried
        logger.info("Transient error detected, will retry: {}", errorMessage);
        throw exception;
    }
    
    /**
     * Determines if the error is a client error (4xx) which should not be retried.
     *
     * @param throwable the exception to check
     * @return true if it's a client error (4xx), false otherwise
     */
    private boolean isClientError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        // Check for WebClient 4xx errors
        if (throwable instanceof WebClientResponseException webClientEx) {
            int statusCode = webClientEx.getStatusCode().value();
            return statusCode >= 400 && statusCode < 500;
        }
        
        // Check cause recursively
        if (throwable.getCause() != null && throwable.getCause() != throwable) {
            return isClientError(throwable.getCause());
        }
        
        return false;
    }
}
