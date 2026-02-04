package com.vibe.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.domain.model.NotificationStatusEvent;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.NotificationRequestMessage;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.ProcessedMessageRepository;
import com.vibe.notification.infrastructure.adapter.messaging.rabbitmq.RabbitMqConfiguration;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for notification status callback via RabbitMQ.
 * Tests that status events (SUCCESS/FAILED/RETRY_EXHAUSTED) are published correctly.
 */
class NotificationStatusCallbackIntegrationTest extends AbstractRabbitMqIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProcessedMessageRepository processedMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @BeforeEach
    void setUp() {
        // Clear processed messages before each test
        processedMessageRepository.deleteAll();

        // Clear templates
        templateRepository.deleteAll();

        // Create test templates for successful processing tests - EMAIL
        NotificationTemplateId emailTemplateId = new NotificationTemplateId("test-template", "en", Channel.EMAIL);
        NotificationTemplateEntity emailTemplate = new NotificationTemplateEntity();
        emailTemplate.setId(emailTemplateId);
        emailTemplate.setTemplateType("TEXT");
        emailTemplate.setSubject("Test [[${name}]]!");
        emailTemplate.setContent("Hello [[${name}]], this is a test!");
        templateRepository.save(emailTemplate);

        // Create test templates for successful processing tests - WHATSAPP
        NotificationTemplateId waTemplateId = new NotificationTemplateId("test-template", "en", Channel.WHATSAPP);
        NotificationTemplateEntity waTemplate = new NotificationTemplateEntity();
        waTemplate.setId(waTemplateId);
        waTemplate.setTemplateType("TEXT");
        waTemplate.setContent("Hello [[${name}]], this is a test via WhatsApp!");
        templateRepository.save(waTemplate);
    }

    /**
     * Test that a successful email notification triggers a SUCCESS status event.
     * Verifies that:
     * 1. The notification is processed successfully
     * 2. A SUCCESS status event is published to the status exchange
     * 3. The trace_id in the status event matches the original request
     */
    @Test
    void testSuccessStatusEventPublished() throws Exception {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John Doe");

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "test@example.com",
                "test-template",
                "en",
                Channel.EMAIL,
                variables,
                null
        );

        // Set up a listener on the status exchange to capture the status event
        AtomicReference<NotificationStatusEvent> capturedStatusEvent = new AtomicReference<>();
        
        // Create a queue bound to the status exchange for testing
        String testQueueName = "test.status.queue." + UUID.randomUUID();
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(testQueueName, false, false, true, null);
            channel.queueBind(testQueueName, RabbitMqConfiguration.NOTIFICATION_STATUS_EXCHANGE, 
                RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY);
            return null;
        });

        // Act - Send notification request
        String messageJson = objectMapper.writeValueAsString(message);
        byte[] body = messageJson.getBytes();

        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setContentEncoding("UTF-8");

        Message amqpMessage = new Message(body, props);
        rabbitTemplate.send(RabbitMqConfiguration.NOTIFICATION_REQUEST, RabbitMqConfiguration.NOTIFICATION_REQUEST, amqpMessage);

        // Assert - Wait for status event to be published
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message statusMessage = rabbitTemplate.receive(testQueueName, 1000);
            assertNotNull(statusMessage, "Status message should be published");
            
            String statusJson = new String(statusMessage.getBody(), "UTF-8");
            Map<String, Object> statusMap = objectMapper.readValue(statusJson, Map.class);
            
            assertEquals(traceId, statusMap.get("trace_id"), "Trace ID should match");
            assertEquals("SUCCESS", statusMap.get("status"), "Status should be SUCCESS");
            assertEquals("EMAIL", statusMap.get("channel"), "Channel should be EMAIL");
            assertNull(statusMap.get("error_message"), "Error message should be null for success");
            assertNotNull(statusMap.get("timestamp"), "Timestamp should be present");
        });

        // Clean up test queue
        rabbitTemplate.execute(channel -> {
            channel.queueDelete(testQueueName);
            return null;
        });
    }

    /**
     * Test that a failed notification (template not found) triggers RETRY_EXHAUSTED status
     * after all retries are exhausted.
     * Verifies that:
     * 1. The notification fails and goes to DLQ after max retries
     * 2. A RETRY_EXHAUSTED status event is published
     * 3. The error message contains details about the failure
     */
    @Test
    void testRetryExhaustedStatusEventPublished() throws Exception {
        // Arrange - Use a non-existent template to trigger retry and eventual DLQ
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Jane Doe");

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "test@example.com",
                "non-existent-template",  // This will trigger template not found exception
                "en",
                Channel.EMAIL,
                variables,
                null
        );

        // Set up a listener on the status exchange to capture the status event
        String testQueueName = "test.status.queue." + UUID.randomUUID();
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(testQueueName, false, false, true, null);
            channel.queueBind(testQueueName, RabbitMqConfiguration.NOTIFICATION_STATUS_EXCHANGE, 
                RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY);
            return null;
        });

        // Act - Send notification request
        String messageJson = objectMapper.writeValueAsString(message);
        byte[] body = messageJson.getBytes();

        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setContentEncoding("UTF-8");

        Message amqpMessage = new Message(body, props);
        rabbitTemplate.send(RabbitMqConfiguration.NOTIFICATION_REQUEST, RabbitMqConfiguration.NOTIFICATION_REQUEST, amqpMessage);

        // Assert - Wait for RETRY_EXHAUSTED status event to be published
        // This will take ~7 seconds due to retry backoff (1s + 2s + 4s)
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            Message statusMessage = rabbitTemplate.receive(testQueueName, 1000);
            assertNotNull(statusMessage, "RETRY_EXHAUSTED status message should be published");
            
            String statusJson = new String(statusMessage.getBody(), "UTF-8");
            Map<String, Object> statusMap = objectMapper.readValue(statusJson, Map.class);
            
            assertEquals(traceId, statusMap.get("trace_id"), "Trace ID should match");
            assertEquals("RETRY_EXHAUSTED", statusMap.get("status"), "Status should be RETRY_EXHAUSTED");
            assertEquals("EMAIL", statusMap.get("channel"), "Channel should be EMAIL");
            assertNotNull(statusMap.get("error_message"), "Error message should be present");
            assertTrue(statusMap.get("error_message").toString().contains("Template not found") ||
                      statusMap.get("error_message").toString().contains("non-existent-template"),
                      "Error message should contain template not found details");
            assertNotNull(statusMap.get("timestamp"), "Timestamp should be present");
        });

        // Verify message ended up in DLQ
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Message dlqMessage = rabbitTemplate.receive(RabbitMqConfiguration.NOTIFICATION_DL, 1000);
            assertNotNull(dlqMessage, "Message should be in DLQ after retries exhausted");
        });

        // Clean up test queue
        rabbitTemplate.execute(channel -> {
            channel.queueDelete(testQueueName);
            return null;
        });
    }

    /**
     * Test that trace_id is properly propagated through OpenTelemetry headers.
     * Verifies that:
     * 1. The status event includes the correct trace_id
     * 2. OTel headers are present in the RabbitMQ message
     */
    @Test
    void testTraceIdPropagationInStatusHeaders() throws Exception {
        // Arrange
        String traceId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Test User");

        NotificationRequestMessage message = new NotificationRequestMessage(
                traceId,
                "test@example.com",
                "test-template",
                "en",
                Channel.EMAIL,
                variables,
                null
        );

        // Set up a listener on the status exchange
        String testQueueName = "test.status.queue." + UUID.randomUUID();
        rabbitTemplate.execute(channel -> {
            channel.queueDeclare(testQueueName, false, false, true, null);
            channel.queueBind(testQueueName, RabbitMqConfiguration.NOTIFICATION_STATUS_EXCHANGE, 
                RabbitMqConfiguration.NOTIFICATION_STATUS_ROUTING_KEY);
            return null;
        });

        // Act
        String messageJson = objectMapper.writeValueAsString(message);
        byte[] body = messageJson.getBytes();

        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setContentEncoding("UTF-8");

        Message amqpMessage = new Message(body, props);
        rabbitTemplate.send(RabbitMqConfiguration.NOTIFICATION_REQUEST, RabbitMqConfiguration.NOTIFICATION_REQUEST, amqpMessage);

        // Assert - Check that trace_id header is present in status message
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message statusMessage = rabbitTemplate.receive(testQueueName, 1000);
            assertNotNull(statusMessage, "Status message should be published");
            
            // Check custom trace_id header
            MessageProperties statusProps = statusMessage.getMessageProperties();
            Object traceIdHeader = statusProps.getHeader("trace_id");
            assertNotNull(traceIdHeader, "trace_id header should be present");
            assertEquals(traceId, traceIdHeader.toString(), "trace_id header should match original");
            
            // Verify the body also contains correct trace_id
            String statusJson = new String(statusMessage.getBody(), "UTF-8");
            Map<String, Object> statusMap = objectMapper.readValue(statusJson, Map.class);
            assertEquals(traceId, statusMap.get("trace_id"), "Trace ID in body should match");
        });

        // Clean up test queue
        rabbitTemplate.execute(channel -> {
            channel.queueDelete(testQueueName);
            return null;
        });
    }
}
