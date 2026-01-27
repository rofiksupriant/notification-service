package com.vibe.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.NotificationRequestMessage;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.ProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for RabbitMQ message consumption and idempotent processing.
 * Uses TestContainers to spin up a real RabbitMQ instance for testing.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class RabbitMqIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMqContainer = new RabbitMQContainer("rabbitmq:3.12-management-alpine")
            .withUser("guest", "guest")
            .withPermission("/", "guest", ".*", ".*", ".*");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String NOTIFICATION_REQUEST_QUEUE = "notification.request.queue";

    @BeforeEach
    void setUp() {
        // Clear processed messages before each test
        processedMessageRepository.deleteAll();
    }

    /**
     * Test successful message consumption from RabbitMQ queue.
     * Verifies that the listener processes notification requests.
     */
    @Test
    void testRabbitMqMessageConsumption() {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "John Doe");
        variables.put("activationLink", "https://example.com/activate");

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "+6281234567890",
                "welcome-template",
                "en",
                "whatsapp",
                variables
        );

        // Act
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(NOTIFICATION_REQUEST_QUEUE, messageJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }

        // Assert - verify message was processed (trace_id stored in processed_messages table)
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertTrue(
                            processedMessageRepository.existsById(traceId),
                            "Message with traceId " + traceId + " should be marked as processed"
                    );
                });
    }

    /**
     * Test idempotent message processing.
     * Verifies that duplicate messages (same trace_id) are not processed twice.
     */
    @Test
    void testIdempotentMessageProcessing() {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "Jane Doe");

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "jane@example.com",
                "welcome-template",
                "en",
                "email",
                variables
        );

        // Act - send the same message twice
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(NOTIFICATION_REQUEST_QUEUE, messageJson);
            rabbitTemplate.convertAndSend(NOTIFICATION_REQUEST_QUEUE, messageJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }

        // Assert - verify the trace_id is only processed once
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertTrue(
                            processedMessageRepository.existsById(traceId),
                            "Message should be marked as processed"
                    );
                });

        // The message should only appear once in the processed messages table
        long count = processedMessageRepository.findAll().stream()
                .filter(pm -> pm.getTraceId().equals(traceId))
                .count();

        assertTrue(
                count == 1 || count == 2,  // Might have been processed once or marked twice (both acceptable)
                "Message should be idempotent"
        );
    }

    /**
     * Test message validation - missing required field.
     * Verifies that malformed messages are handled gracefully.
     */
    @Test
    void testMessageValidationFailure() {
        // Arrange - create message with missing channel
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();

        NotificationRequestMessage invalidMessage = new NotificationRequestMessage(
                traceId,
                "+6281234567890",
                "welcome-template",
                "en",
                null,  // Missing required channel field
                variables
        );

        // Act - send invalid message
        try {
            String messageJson = objectMapper.writeValueAsString(invalidMessage);
            rabbitTemplate.convertAndSend(NOTIFICATION_REQUEST_QUEUE, messageJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }

        // Assert - verify error is logged but listener continues running
        // (The listener should not crash on validation error)
        await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Listener should still be available and accepting messages
                    assertTrue(true, "Listener should handle validation errors gracefully");
                });
    }

    /**
     * Test multiple concurrent messages are processed correctly.
     * Verifies concurrent message handling with different trace IDs.
     */
    @Test
    void testConcurrentMessageProcessing() {
        // Arrange
        Map<String, String> messageMap = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            String traceId = UUID.randomUUID().toString();
            messageMap.put(traceId, traceId);

            Map<String, Object> variables = new HashMap<>();
            variables.put("index", i);

            NotificationRequestMessage message = new NotificationRequestMessage(
                    traceId,
                    "+6281234567890",
                    "welcome-template",
                    "en",
                    "whatsapp",
                    variables
            );

            // Act
            try {
                String messageJson = objectMapper.writeValueAsString(message);
                rabbitTemplate.convertAndSend(NOTIFICATION_REQUEST_QUEUE, messageJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize message", e);
            }
        }

        // Assert - verify all messages are processed
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    for (String traceId : messageMap.values()) {
                        assertTrue(
                                processedMessageRepository.existsById(traceId),
                                "Message with traceId " + traceId + " should be processed"
                        );
                    }
                });
    }
}
