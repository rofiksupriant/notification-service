package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializer for RabbitMQ queues.
 * 
 * Automatically deletes old queue versions on application startup to:
 * 1. Prevent PRECONDITION_FAILED errors when queue arguments change
 * 2. Ensure fresh queue creation with correct DLX configuration
 * 3. Avoid conflicts between listener auto-declaration and bean configuration
 */
@Component
public class RabbitQueueInitializer {
    private static final Logger logger = LoggerFactory.getLogger(RabbitQueueInitializer.class);

    private static final String[] OLD_QUEUES = {
        RabbitMqConfiguration.NOTIFICATION_REQUEST,
        RabbitMqConfiguration.NOTIFICATION_DL
    };

    private static final String[] OLD_EXCHANGES = {
        RabbitMqConfiguration.NOTIFICATION_REQUEST,
        RabbitMqConfiguration.NOTIFICATION_DL
    };

    public RabbitQueueInitializer(RabbitAdmin rabbitAdmin) {
        logger.info("Initializing RabbitMQ queue cleanup...");
        
        // Delete old queue versions
        for (String queueName : OLD_QUEUES) {
            try {
                rabbitAdmin.deleteQueue(queueName);
                logger.info("Deleted old queue: {}", queueName);
            } catch (Exception e) {
                logger.debug("Queue {} not found or already deleted: {}", queueName, e.getMessage());
            }
        }

        // Delete old exchange versions
        for (String exchangeName : OLD_EXCHANGES) {
            try {
                rabbitAdmin.deleteExchange(exchangeName);
                logger.info("Deleted old exchange: {}", exchangeName);
            } catch (Exception e) {
                logger.debug("Exchange {} not found or already deleted: {}", exchangeName, e.getMessage());
            }
        }

        logger.info("RabbitMQ queue initialization completed");
    }
}
