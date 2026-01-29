package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RabbitMQ configuration for inbound notification requests with retry mechanism and DLQ support.
 * Configures the queue, DLQ, and retry policy for resilient message processing.
 *
 * Features:
 * - Exponential backoff retry (1s, 2s, 4s)
 * - Maximum 3 retry attempts
 * - Dead Letter Queue (DLQ) for failed messages
 * - Differentiation between transient and client errors
 *
 * This configuration is conditionally enabled via the feature toggle:
 * {@code app.feature.rabbitmq.enabled=true}
 */
@Configuration
@ConditionalOnProperty(name = "app.feature.rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfiguration {

    public static final String NOTIFICATION_REQUEST_QUEUE = "notification.request.queue";
    public static final String NOTIFICATION_DLQ = "notification.dlq";
    public static final String NOTIFICATION_DLX = "notification.dlx";
    public static final String NOTIFICATION_REQUEST_EXCHANGE = "notification.request.exchange";
    public static final String NOTIFICATION_REQUEST_ROUTING_KEY = "notification.request.*";

    /**
     * Declares the Dead Letter Exchange (DLX).
     * Messages that fail after max retries will be routed here.
     *
     * @return the DLX exchange bean
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    /**
     * Declares the Dead Letter Queue (DLQ).
     * Stores messages that have exhausted all retry attempts.
     *
     * @return the configured DLQ bean
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    /**
     * Binds the DLQ to the DLX.
     *
     * @return the binding bean
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(NOTIFICATION_REQUEST_QUEUE);
    }

    /**
     * Declares the notification request queue with DLX configuration.
     * Queue is durable and will persist messages even if RabbitMQ restarts.
     * Failed messages are routed to the DLX after max retries.
     *
     * @return the configured queue bean
     */
    @Bean
    public Queue notificationRequestQueue() {
        return QueueBuilder.durable(NOTIFICATION_REQUEST_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_REQUEST_QUEUE)
                .build();
    }

    /**
     * Configures the message converter for JSON deserialization.
     * Enables automatic conversion of JSON messages to Java objects (records).
     *
     * @return Jackson2 message converter bean
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Configures the listener container factory with retry policy.
     * Implements exponential backoff with max 3 attempts.
     *
     * @param connectionFactory the RabbitMQ connection factory
     * @return the configured container factory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            RabbitTemplate rabbitTemplate) {
        
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        
        // Configure retry interceptor
        factory.setAdviceChain(retryInterceptor(rabbitTemplate));
        
        return factory;
    }

    /**
     * Creates a retry interceptor with exponential backoff policy.
     *
     * @return the configured retry interceptor
     */
    private org.aopalliance.intercept.MethodInterceptor retryInterceptor(RabbitTemplate rabbitTemplate) {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Exponential backoff: 1s, 2s, 4s
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);  // 1 second
        backOffPolicy.setMultiplier(2.0);          // Double each time
        backOffPolicy.setMaxInterval(4000L);       // Max 4 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Max 3 attempts total (1 initial + 2 retries)
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Custom message recoverer that adds error headers to DLQ messages
        DlqMessageRecoverer messageRecoverer = new DlqMessageRecoverer(
            rabbitTemplate,
            NOTIFICATION_DLX,
            NOTIFICATION_REQUEST_QUEUE,
            NOTIFICATION_REQUEST_QUEUE
        );
        
        return org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
                .stateless()
                .retryOperations(retryTemplate)
                .recoverer(messageRecoverer)
                .build();
    }
}

