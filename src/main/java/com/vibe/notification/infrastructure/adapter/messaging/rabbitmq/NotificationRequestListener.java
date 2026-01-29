package com.vibe.notification.infrastructure.adapter.messaging.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.vibe.notification.application.NotificationApplicationService;
import com.vibe.notification.application.dto.SendNotificationRequest;

/**
 * RabbitMQ listener for processing inbound notification requests.
 * Consumes messages from the notification.request.queue and delegates to the notification service.
 *
 * Features:
 * - Idempotent processing: Uses trace_id to prevent duplicate handling
 * - Graceful error handling: Logs parsing errors without failing the consumer
 * - Conditional enablement: Only active when app.feature.rabbitmq.enabled=true
 *
 * This component automatically acknowledges messages after successful processing.
 * Failed messages can be requeued based on Spring AMQP configuration.
 */
@Component
@ConditionalOnProperty(name = "app.feature.rabbitmq.enabled", havingValue = "true")
public class NotificationRequestListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRequestListener.class);

    private final NotificationApplicationService notificationApplicationService;
    private final ProcessedMessageRepository processedMessageRepository;

    public NotificationRequestListener(
        NotificationApplicationService notificationApplicationService,
        ProcessedMessageRepository processedMessageRepository
    ) {
        this.notificationApplicationService = notificationApplicationService;
        this.processedMessageRepository = processedMessageRepository;
    }

    /**
     * Listens for notification request messages on the notification.request.queue.
     * Ensures idempotent processing by checking if the trace_id has been processed.
     *
     * @param message the notification request message containing recipient, template slug, language, and variables
     */
    @RabbitListener(
        queues = RabbitMqConfiguration.NOTIFICATION_REQUEST_QUEUE,
        errorHandler = "notificationErrorHandler"
    )
    public void handleNotificationRequest(NotificationRequestMessage message) {
        // Check if message has already been processed using trace_id
        if (isMessageAlreadyProcessed(message.traceId())) {
            logger.debug("Message with trace_id {} already processed, skipping", message.traceId());
            return;
        }

        // Validate incoming message
        validateMessage(message);

        // Convert to application request and process
        // Use message's trace_id as idempotency key
        SendNotificationRequest request = new SendNotificationRequest(
            message.recipient(),
            message.slug(),
            message.language(),
            message.channel(),
            message.variables(),
            java.util.Optional.of(message.traceId())
        );

        // Send notification asynchronously
        notificationApplicationService.sendNotification(request);

        // Mark message as processed
        markMessageAsProcessed(message.traceId());

        logger.info("Notification request processed successfully. Trace ID: {}, Recipient: {}, Template: {}",
            message.traceId(), message.recipient(), message.slug());
    }

    /**
     * Checks if a message with the given trace_id has already been processed.
     *
     * @param traceId the unique message identifier
     * @return true if the message was previously processed, false otherwise
     */
    private boolean isMessageAlreadyProcessed(String traceId) {
        return processedMessageRepository.existsById(traceId);
    }

    /**
     * Records that a message has been successfully processed.
     * Uses saveAndFlush to immediately persist and prevent race conditions.
     *
     * @param traceId the unique message identifier
     */
    private void markMessageAsProcessed(String traceId) {
        try {
            // Check again before saving to handle race conditions
            if (!processedMessageRepository.existsById(traceId)) {
                processedMessageRepository.saveAndFlush(new ProcessedMessage(traceId));
            }
        } catch (Exception e) {
            // Silently ignore if message was already saved by another thread
            logger.debug("Message with trace_id {} already marked as processed", traceId);
        }
    }

    /**
     * Validates the incoming message payload.
     * Ensures all required fields are present and valid.
     *
     * @param message the message to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateMessage(NotificationRequestMessage message) {
        if (message.traceId() == null || message.traceId().isBlank()) {
            throw new IllegalArgumentException("trace_id is required");
        }
        if (message.recipient() == null || message.recipient().isBlank()) {
            throw new IllegalArgumentException("recipient is required");
        }
        if (message.slug() == null || message.slug().isBlank()) {
            throw new IllegalArgumentException("slug is required");
        }
        if (message.language() == null || message.language().isBlank()) {
            throw new IllegalArgumentException("language is required");
        }
        if (message.channel() == null || message.channel().isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }
        if (message.variables() == null) {
            throw new IllegalArgumentException("variables map is required");
        }
    }
}

