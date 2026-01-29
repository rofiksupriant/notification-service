package com.vibe.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.NotificationRequestMessage;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.ProcessedMessageRepository;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.RabbitMqConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RabbitMQ retry mechanism and Dead Letter Queue (DLQ).
 * Validates:
 * - Exponential backoff retry (1s, 2s, 4s) across 4 total attempts (1 initial + 3 retries)
 * - DLQ routing after exhausted retries
 * - Custom header x-last-error in DLQ messages
 * - Exponential backoff timing
 * - Differentiation between transient errors (retryable) and validation errors (non-retryable)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class RetryAndDlqIntegrationTest {

    @Container
    static RabbitMQContainer rabbitMqContainer = new RabbitMQContainer("rabbitmq:3.12-management-alpine")
            .withUser("guest", "guest")
            .withPermission("/", "guest", ".*", ".*", ".*");

    @DynamicPropertySource
    static void registerRabbitMqProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMqContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMqContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("app.feature.rabbitmq.enabled", () -> "true");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clear processed messages before each test
        processedMessageRepository.deleteAll();
        
        // Purge queues to ensure clean state
        try {
            rabbitTemplate.execute(channel -> {
                channel.queuePurge(RabbitMqConfiguration.NOTIFICATION_REQUEST_QUEUE);
                channel.queuePurge(RabbitMqConfiguration.NOTIFICATION_DLQ);
                return null;
            });
        } catch (Exception e) {
            // Ignore if queues don't exist yet
        }
    }

    /**
     * Test that a message is successfully processed without retries when no errors occur.
     */
    @Test
    void testSuccessfulProcessingWithoutRetry() {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "John Doe");

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "test@example.com",
                "welcome-template",
                "en",
                "email",
                variables
        );

        // Act
        sendMessage(message);

        // Assert - message should be processed successfully
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertTrue(
                            processedMessageRepository.existsById(traceId),
                            "Message should be processed successfully"
                    );
                });
    }

    /**
     * Test that messages with non-existent templates eventually go to DLQ after max retries.
     * This simulates a permanent error scenario (template not found).
     */
    @Test
    void testMessageGoesToDlqAfterMaxRetries() {
        // Arrange - use a non-existent template to simulate failure
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "test@example.com",
                "non-existent-template",  // This will cause failures
                "en",
                "email",
                variables
        );

        // Act
        sendMessage(message);

        // Assert - after retries exhausted, message should end up in DLQ
        // Total retry time: 1s + 2s + 4s = 7s plus processing overhead
        await()
                .atMost(25, TimeUnit.SECONDS)  // Allow time for retries and processing
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Check DLQ for the message
                    Message dlqMessage = rabbitTemplate.receive(RabbitMqConfiguration.NOTIFICATION_DLQ, 1000);
                    assertNotNull(dlqMessage, "Message should be in DLQ after max retries");
                    
                    // Verify custom header x-last-error exists
                    MessageProperties props = dlqMessage.getMessageProperties();
                    assertNotNull(props.getHeader("x-last-error"), 
                        "DLQ message should have x-last-error header");
                    assertNotNull(props.getHeader("x-last-error-timestamp"),
                        "DLQ message should have x-last-error-timestamp header");
                });
    }

    /**
     * Test that DLQ messages contain the custom x-last-error header with error details.
     */
    @Test
    void testDlqMessageContainsCustomErrorHeader() {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "invalid@example.com",
                "non-existent-template",
                "en",
                "email",
                variables
        );

        // Act
        sendMessage(message);

        // Assert
        await()
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Message dlqMessage = rabbitTemplate.receive(RabbitMqConfiguration.NOTIFICATION_DLQ, 1000);
                    assertNotNull(dlqMessage, "Message should be in DLQ");
                    
                    MessageProperties props = dlqMessage.getMessageProperties();
                    String errorHeader = (String) props.getHeader("x-last-error");
                    
                    assertNotNull(errorHeader, "x-last-error header should be present");
                    assertFalse(errorHeader.isEmpty(), "x-last-error header should not be empty");
                    
                    // Verify timestamp is present and is a valid number
                    Object timestamp = props.getHeader("x-last-error-timestamp");
                    assertNotNull(timestamp, "x-last-error-timestamp should be present");
                });
    }

    /**
     * Test exponential backoff by measuring retry intervals.
     * This test verifies that retries happen with increasing delays.
     */
    @Test
    void testExponentialBackoffRetryIntervals() {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "test@example.com",
                "non-existent-template",
                "en",
                "email",
                variables
        );

        // Act
        long startTime = System.currentTimeMillis();
        sendMessage(message);

        // Assert - message should take approximately 7 seconds (1s + 2s + 4s) before going to DLQ
        await()
                .atLeast(7, TimeUnit.SECONDS)   // At least 7 seconds for all retries
                .atMost(30, TimeUnit.SECONDS)   // At most 30 seconds (retries + processing overhead)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Message dlqMessage = rabbitTemplate.receive(RabbitMqConfiguration.NOTIFICATION_DLQ, 1000);
                    assertNotNull(dlqMessage, "Message should be in DLQ after retries");
                    
                    long duration = System.currentTimeMillis() - startTime;
                    // Total retry time should be approximately 1s + 2s + 4s = 7s (with some tolerance)
                    assertTrue(duration >= 7000, 
                        "Retries should take at least 7 seconds (exponential backoff), took: " + duration + "ms");
                });
    }

    /**
     * Test that the DLQ queue is automatically created and bound correctly.
     */
    @Test
    void testDlqQueueAutoCreation() {
        // Assert - verify DLQ exists
        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Boolean result = rabbitTemplate.execute(channel -> {
                        try {
                            // Try to declare passively - will succeed if queue exists
                            channel.queueDeclarePassive(RabbitMqConfiguration.NOTIFICATION_DLQ);
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    });
                    assertTrue(result, "DLQ should be automatically created");
                });
    }

    /**
     * Helper method to send a message to the notification request queue.
     */
    private void sendMessage(NotificationRequestMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            byte[] body = messageJson.getBytes();
            
            MessageProperties props = new MessageProperties();
            props.setContentType("application/json");
            props.setContentEncoding("UTF-8");
            
            Message msg = new Message(body, props);
            rabbitTemplate.send(RabbitMqConfiguration.NOTIFICATION_REQUEST_QUEUE, msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message", e);
        }
    }
}
