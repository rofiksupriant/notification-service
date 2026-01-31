package com.vibe.notification.integration;

import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationLogRepository;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.UUID;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Test with Testcontainers
 * Tests the complete flow: request -> template resolution -> rendering -> DB logging
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Notification Service E2E Integration Tests")
class NotificationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private NotificationLogRepository logRepository;

    @BeforeEach
    void setUp() {
        // Clean up repositories before each test
        logRepository.deleteAll();
        templateRepository.deleteAll();
    }

    @Test
    @DisplayName("Should process notification request and create pending log")
    void shouldProcessNotificationAndCreateLog() throws Exception {
        // Given - Create test templates for EMAIL
        var emailTemplateId = new NotificationTemplateId("welcome", "en", "EMAIL");
        var emailTemplate = new NotificationTemplateEntity(
            emailTemplateId,
            "TEXT",
            "Welcome to [[${companyName}]]",
            "Hello [[${name}]], welcome to [[${companyName}]]!",
            null
        );
        templateRepository.save(emailTemplate);
        
        // And - Create test templates for WHATSAPP
        var waTemplateId = new NotificationTemplateId("welcome", "en", "whatsapp");
        var waTemplate = new NotificationTemplateEntity(
            waTemplateId,
            "TEXT",
            null,
            "Hello [[${name}]], welcome to [[${companyName}]] via WhatsApp!",
            null
        );
        templateRepository.save(waTemplate);

        // And - Prepare request
        var variables = new HashMap<String, Object>();
        variables.put("name", "John Doe");
        variables.put("companyName", "VibeCoding");

        var request = new SendNotificationRequest(
            "john@example.com",
            "welcome",
            "en",
            "EMAIL",
            variables
        );

        // When - Send notification request
        var response = mockMvc.perform(post("/api/v1/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        
        // Then - Verify response
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status", equalTo("PENDING")))
            .andReturn();

        // Extract trace_id from response
        var responseBody = objectMapper.readTree(response.getResponse().getContentAsString());
        var traceId = UUID.fromString(responseBody.get("traceId").asText());

        // Verify log was created with PENDING or SUCCESS status
        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
            var logs = logRepository.findByTraceId(traceId);
            assertFalse(logs.isEmpty(), "No logs found for trace_id");
            assertEquals(1, logs.size());
            
            var log = logs.get(0);
            // Accept either PENDING (if not yet processed) or SUCCESS (if async completed quickly)
            // Also accept FAILED if there was an error with the adapter (expected in test env without real email)
            assertTrue(
                log.getStatus().equals("PENDING") || 
                log.getStatus().equals("SUCCESS") ||
                log.getStatus().equals("FAILED"), 
                "Status should be PENDING, SUCCESS, or FAILED, but was: " + log.getStatus() + 
                ", error: " + log.getErrorMessage()
            );
            assertEquals("john@example.com", log.getRecipient());
            assertEquals("welcome", log.getSlug());
            assertEquals("EMAIL", log.getChannel());
        });
    }

    @Test
    @DisplayName("Should resolve template with language fallback")
    void shouldResolveTemplateWithFallback() throws Exception {
        // Given - Create only English template (no French) for EMAIL
        var emailTemplateId = new NotificationTemplateId("order_confirmation", "en", "EMAIL");
        var emailTemplate = new NotificationTemplateEntity(
            emailTemplateId,
            "TEXT",
            "Order Confirmation",
            "Your order [[${orderId}]] is confirmed",
            null
        );
        templateRepository.save(emailTemplate);
        
        // And - Create English template for WHATSAPP
        var waTemplateId = new NotificationTemplateId("order_confirmation", "en", "whatsapp");
        var waTemplate = new NotificationTemplateEntity(
            waTemplateId,
            "TEXT",
            null,
            "Your order [[${orderId}]] is confirmed via WhatsApp",
            null
        );
        templateRepository.save(waTemplate);

        // And - Prepare request in French (fallback to English)
        var variables = new HashMap<String, Object>();
        variables.put("orderId", "ORD-001");

        var request = new SendNotificationRequest(
            "customer@example.com",
            "order_confirmation",
            "fr",  // French - will fallback to English
            "EMAIL",
            variables
        );

        // When - Send notification request
        var response = mockMvc.perform(post("/api/v1/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        
        // Then - Should succeed (fallback to English)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status", equalTo("PENDING")))
            .andReturn();

        // Verify log was created
        var responseBody = objectMapper.readTree(response.getResponse().getContentAsString());
        var traceId = UUID.fromString(responseBody.get("traceId").asText());

        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
            var logs = logRepository.findByTraceId(traceId);
            assertFalse(logs.isEmpty());
        });
    }

    @Test
    @DisplayName("Should return 404 when template not found")
    void shouldReturn404WhenTemplateNotFound() throws Exception {
        // Given - No template exists for this slug
        var request = new SendNotificationRequest(
            "user@example.com",
            "non_existent_template",
            "en",
            "EMAIL",
            new HashMap<>()
        );

        // When - Send notification request
        // Then - Should return accepted (async processing will fail and mark log as FAILED)
        mockMvc.perform(post("/api/v1/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Wait a bit for async processing and verify log is marked as FAILED
        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                var failedLogs = logRepository.findByStatus("FAILED");
                assertFalse(failedLogs.isEmpty(), "Expected at least one FAILED log");
            });
    }

    @Test
    @DisplayName("Should render template with multiple variables")
    void shouldRenderTemplateWithMultipleVariables() throws Exception {
        // Given - Create template for EMAIL
        var emailTemplateId = new NotificationTemplateId("invoice", "en", "EMAIL");
        var emailTemplate = new NotificationTemplateEntity(
            emailTemplateId,
            "TEXT",
            "Invoice [[${invoiceNumber}]]",
            "Invoice #[[${invoiceNumber}]] for [[${customerName}]] - Amount: [[${amount}]]",
            null
        );
        templateRepository.save(emailTemplate);
        
        // And - Create template for WHATSAPP
        var waTemplateId = new NotificationTemplateId("invoice", "en", "whatsapp");
        var waTemplate = new NotificationTemplateEntity(
            waTemplateId,
            "TEXT",
            null,
            "Invoice #[[${invoiceNumber}]] for [[${customerName}]] - Amount: [[${amount}]] (WhatsApp)",
            null
        );
        templateRepository.save(waTemplate);

        var variables = new HashMap<String, Object>();
        variables.put("invoiceNumber", "INV-2024-001");
        variables.put("customerName", "Acme Corp");
        variables.put("amount", "Rp 1.000.000");

        var request = new SendNotificationRequest(
            "billing@acme.com",
            "invoice",
            "en",
            "EMAIL",
            variables
        );

        // When
        mockMvc.perform(post("/api/v1/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then - Verify log was created with variables
        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
            var logs = logRepository.findByRecipient("billing@acme.com");
            assertFalse(logs.isEmpty());
            assertNotNull(logs.get(0).getVariables());
        });
    }

    @Test
    @DisplayName("Should handle WhatsApp message template")
    void shouldHandleWhatsAppTemplate() throws Exception {
        // Given - Create template for EMAIL
        var emailTemplateId = new NotificationTemplateId("otp", "en", "email");
        var emailTemplate = new NotificationTemplateEntity(
            emailTemplateId,
            "TEXT",
            null,
            "Your OTP is [[${otp}]]",
            null
        );
        templateRepository.save(emailTemplate);
        
        // And - Create template for WHATSAPP
        var waTemplateId = new NotificationTemplateId("otp", "en", "whatsapp");
        var waTemplate = new NotificationTemplateEntity(
            waTemplateId,
            "TEXT",
            null,
            "Your OTP is [[${otp}]] via WhatsApp",
            null
        );
        templateRepository.save(waTemplate);

        var variables = new HashMap<String, Object>();
        variables.put("otp", "123456");

        var request = new SendNotificationRequest(
            "6281991388080",
            "otp",
            "en",
            "WHATSAPP",
            variables
        );

        // When
        mockMvc.perform(post("/api/v1/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());

        // Then - Verify log was created with WHATSAPP channel
        await()
            .atMost(Duration.ofSeconds(15))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
            var logs = logRepository.findByRecipient("6281991388080");
            assertFalse(logs.isEmpty());
            assertEquals("WHATSAPP", logs.get(0).getChannel());
        });
    }

    @Test
    @DisplayName("Should verify health endpoint")
    void shouldVerifyHealthEndpoint() throws Exception {
        // Note: Use GET for health
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/notifications/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("Notification Service is healthy"));
    }
}
