package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import com.vibe.notification.domain.port.NotificationStatusProducer;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
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

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ configuration for inbound notification requests with retry mechanism
 * and DLQ support.
 * Configures the queue, DLQ, and retry policy for resilient message processing.
 *
 * Features:
 * - Exponential backoff retry (1s, 2s, 4s) across 4 total attempts (1 initial +
 * 3 retries)
 * - Dead Letter Queue (DLQ) for failed messages
 * - Differentiation between transient errors (retryable) and validation errors
 * (non-retryable)
 *
 * This configuration is conditionally enabled via the feature toggle:
 * {@code app.feature.rabbitmq.enabled=true}
 */
@Configuration
@ConditionalOnProperty(name = "app.feature.rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfiguration {

    public static final String NOTIFICATION_REQUEST = "notification.request";
    public static final String NOTIFICATION_DL = "notification.dl";
    public static final String NOTIFICATION_STATUS_EXCHANGE = "notification.status.exchange";
    public static final String NOTIFICATION_STATUS_ROUTING_KEY = "status.updated";

    /**
     * Creates RabbitAdmin for managing RabbitMQ resources.
     * Used for auto-deleting old queues on startup.
     *
     * @return the RabbitAdmin bean
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Declares the Main Exchange.
     * Messages that come from client will be routed here.
     *
     * @return the main exchange bean
     */
    @Bean
    public DirectExchange mainExchange() {
        return new DirectExchange(NOTIFICATION_REQUEST, true, false);
    }

    /**
     * Declares the Main Queue.
     * Stores messages that come from client.
     *
     * @return the configured main queue bean
     */
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(NOTIFICATION_REQUEST)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DL)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DL)
                .build();
    }

    /**
     * Binds the Main Queue to the Main Exchange.
     *
     * @return the binding bean
     */
    @Bean
    public Binding mainBinding() {
        return BindingBuilder
                .bind(mainQueue())
                .to(mainExchange())
                .with(NOTIFICATION_REQUEST);
    }

    /**
     * Declares the Dead Letter Exchange (DLX).
     * Messages that fail after max retries will be routed here.
     *
     * @return the DLX exchange bean
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DL, true, false);
    }

    /**
     * Declares the Dead Letter Queue (DLQ).
     * Stores messages that have exhausted all retry attempts.
     *
     * @return the configured DLQ bean
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DL).build();
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
                .with(NOTIFICATION_DL);
    }

    /**
     * Declares the Notification Status Exchange.
     * Status events (SUCCESS/FAILED/RETRY_EXHAUSTED) are published here.
     * Topic exchange allows flexible routing patterns.
     *
     * @return the notification status exchange bean
     */
    @Bean
    public TopicExchange notificationStatusExchange() {
        return new TopicExchange(NOTIFICATION_STATUS_EXCHANGE, true, false);
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
            RabbitTemplate rabbitTemplate,
            NotificationStatusProducer notificationStatusProducer,
            ObjectMapper objectMapper) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        // Configure retry interceptor
        factory.setAdviceChain(retryInterceptor(rabbitTemplate, notificationStatusProducer, objectMapper));

        return factory;
    }

    /**
     * Creates a retry interceptor with exponential backoff policy.
     *
     * @return the configured retry interceptor
     */
    private org.aopalliance.intercept.MethodInterceptor retryInterceptor(
            RabbitTemplate rabbitTemplate,
            NotificationStatusProducer notificationStatusProducer,
            ObjectMapper objectMapper) {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Exponential backoff: 1s, 2s, 4s
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L); // 1 second
        backOffPolicy.setMultiplier(2.0); // Double each time
        backOffPolicy.setMaxInterval(4000L); // Max 4 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Max 4 attempts total (1 initial + 3 retries) to achieve 1s, 2s, 4s backoff
        // sequence
        // Don't retry validation errors (AmqpRejectAndDontRequeueException)
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(Exception.class, true);
        retryableExceptions.put(org.springframework.amqp.AmqpRejectAndDontRequeueException.class, false);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(4, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Custom message recoverer that adds error headers to DLQ messages and publishes RETRY_EXHAUSTED status
        DlqMessageRecoverer messageRecoverer = new DlqMessageRecoverer(
                rabbitTemplate,
                NOTIFICATION_DL,
                NOTIFICATION_DL,
                NOTIFICATION_REQUEST,
                notificationStatusProducer,
                objectMapper);

        return org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
                .stateless()
                .retryOperations(retryTemplate)
                .recoverer(messageRecoverer)
                .build();
    }
}
