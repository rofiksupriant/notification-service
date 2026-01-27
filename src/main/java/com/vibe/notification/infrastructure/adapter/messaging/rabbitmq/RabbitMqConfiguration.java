package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for inbound notification requests.
 * Configures the queue, exchange, and bindings required for asynchronous message processing.
 *
 * This configuration is conditionally enabled via the feature toggle:
 * {@code app.feature.rabbitmq.enabled=true}
 */
@Configuration
@ConditionalOnProperty(name = "app.feature.rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfiguration {

    public static final String NOTIFICATION_REQUEST_QUEUE = "notification.request.queue";
    public static final String NOTIFICATION_REQUEST_EXCHANGE = "notification.request.exchange";
    public static final String NOTIFICATION_REQUEST_ROUTING_KEY = "notification.request.*";

    /**
     * Declares the notification request queue.
     * Queue is durable and will persist messages even if RabbitMQ restarts.
     *
     * @return the configured queue bean
     */
    @Bean
    public Queue notificationRequestQueue() {
        return new Queue(NOTIFICATION_REQUEST_QUEUE, true);
    }
}
