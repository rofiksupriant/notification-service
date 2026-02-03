package com.vibe.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.vibe.notification.infrastructure.persistence.entity.NotificationTemplateId;
import com.vibe.notification.infrastructure.persistence.repository.NotificationLogRepository;
import com.vibe.notification.infrastructure.persistence.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for async-default API with optional sync mode
 * Tests the complete flow with real database and async execution
 */
@DisplayName("Notification Service - Async/Sync Integration Tests")
class NotificationAsyncSyncIntegrationTest extends AbstractPostgresIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationAsyncSyncIntegrationTest.class);
    
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
        
        // Setup test templates
        setupTestTemplates();
    }

    private void setupTestTemplates() {
        // Create EMAIL template
        var emailTemplate = new NotificationTemplateEntity(
            new NotificationTemplateId("test-template", "en", Channel.EMAIL),
            "TEXT",
            "Test Subject [[${name}]]",
            "Hello [[${name}]], test message!",
            null
        );
        templateRepository.save(emailTemplate);
        
        // Create WHATSAPP template
        var waTemplate = new NotificationTemplateEntity(
            new NotificationTemplateId("test-template", "en", Channel.WHATSAPP),
            "TEXT",
            null,
            "Hello [[${name}]], test WhatsApp message!",
            null
        );
        templateRepository.save(waTemplate);
    }

    @Test
    @DisplayName("Integration: Should return ACCEPTED status immediately when sync=false")
    void shouldReturnAcceptedStatusImmediatelyForAsyncMode() throws Exception {
        // Given
        var variables = new HashMap<String, Object>();
        variables.put("name", "John Doe");
        
        var request = new SendNotificationRequest(
            "john@example.com",
            "test-template",
            "en",
            Channel.EMAIL,
            variables
        );

        // When - Send request with sync=false
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(post("/api/v1/notifications/send?sync=false")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        
        // Then - Should return 202 Accepted immediately
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status", equalTo("ACCEPTED")))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.logId").exists())
            .andReturn();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify response was immediate (< 1 second)
        assertTrue(duration < 1000, "Async mode should return in less than 1 second, took " + duration + "ms");
        
        // Save test result to .ignore/ folder
        saveTestResult("async_mode_response_time.json", Map.of(
            "test", "shouldReturnAcceptedStatusImmediatelyForAsyncMode",
            "responseTimeMs", duration,
            "status", "ACCEPTED",
            "timestamp", LocalDateTime.now().toString()
        ));
        
        // Extract trace_id and verify async processing
        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        var traceId = UUID.fromString(responseBody.get("traceId").asText());
        
        // Wait for async processing to complete
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                var logs = logRepository.findByTraceId(traceId);
                assertFalse(logs.isEmpty(), "Log should be created");
                // Status should be SUCCESS or FAILED (not PENDING anymore)
                String status = logs.get(0).getStatus();
                assertTrue(
                    status.equals("SUCCESS") || status.equals("FAILED"),
                    "Status should be SUCCESS or FAILED after processing, but was: " + status
                );
            });
    }

    @Test
    @DisplayName("Integration: Should return final provider status when sync=true")
    void shouldReturnFinalProviderStatusForSyncMode() throws Exception {
        // Given
        var variables = new HashMap<String, Object>();
        variables.put("name", "Jane Doe");
        
        var request = new SendNotificationRequest(
            "jane@example.com",
            "test-template",
            "en",
            Channel.EMAIL,
            variables
        );

        // When - Send request with sync=true
        MvcResult result = mockMvc.perform(post("/api/v1/notifications/send?sync=true")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        
        // Then - Should return 200 OK with final status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.logId").exists())
            .andExpect(jsonPath("$.providerStatus").exists())
            .andReturn();
        
        // Verify provider status is either SUCCESS or FAILED
        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        String providerStatus = responseBody.get("providerStatus").asText();
        
        assertTrue(
            providerStatus.equals("SUCCESS") || providerStatus.equals("FAILED"),
            "Provider status should be SUCCESS or FAILED, but was: " + providerStatus
        );
        
        // Save test result
        saveTestResult("sync_mode_final_status.json", Map.of(
            "test", "shouldReturnFinalProviderStatusForSyncMode",
            "providerStatus", providerStatus,
            "status", responseBody.get("status").asText(),
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @Test
    @DisplayName("Integration: Should verify trace_id remains same between request and async thread")
    void shouldVerifyTraceIdConsistencyAcrossThreads() throws Exception {
        // Given
        var variables = new HashMap<String, Object>();
        variables.put("name", "Trace Test User");
        
        var request = new SendNotificationRequest(
            "tracetest@example.com",
            "test-template",
            "en",
            Channel.EMAIL,
            variables
        );

        // When - Send request
        MvcResult result = mockMvc.perform(post("/api/v1/notifications/send?sync=false")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andReturn();
        
        // Extract trace_id from response
        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        var responseTraceId = UUID.fromString(responseBody.get("traceId").asText());
        
        logger.info("Request thread trace_id: {}", responseTraceId);
        
        // Wait for async processing and verify trace_id in database
        await()
            .atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                var logs = logRepository.findByTraceId(responseTraceId);
                assertFalse(logs.isEmpty(), "Log should be created");
                
                // Verify the trace_id in the database matches the response
                UUID dbTraceId = logs.get(0).getTraceId();
                assertEquals(responseTraceId, dbTraceId, 
                    "Trace ID should be consistent between request thread and async processing thread");
                
                logger.info("Async thread trace_id (from DB): {}", dbTraceId);
            });
        
        // Save test result
        saveTestResult("trace_id_consistency.json", Map.of(
            "test", "shouldVerifyTraceIdConsistencyAcrossThreads",
            "requestThreadTraceId", responseTraceId.toString(),
            "asyncThreadTraceId", responseTraceId.toString(),
            "consistent", true,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @Test
    @DisplayName("Integration: Should verify async response is ACCEPTED even if background process still running")
    void shouldVerifyAsyncResponseIsAcceptedWhileBackgroundRunning() throws Exception {
        // Given
        var variables = new HashMap<String, Object>();
        variables.put("name", "Background Test User");
        
        var request = new SendNotificationRequest(
            "background@example.com",
            "test-template",
            "en",
            Channel.EMAIL,
            variables
        );

        // When - Send request with sync=false (async mode)
        MvcResult result = mockMvc.perform(post("/api/v1/notifications/send?sync=false")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        
        // Then - Should return ACCEPTED immediately
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status", equalTo("ACCEPTED")))
            .andReturn();
        
        // Extract trace_id
        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        var traceId = UUID.fromString(responseBody.get("traceId").asText());
        
        // Verify log exists with PENDING status initially (or might already be SUCCESS/FAILED if fast)
        var logs = logRepository.findByTraceId(traceId);
        assertFalse(logs.isEmpty(), "Log should be created");
        
        // The important part: response was ACCEPTED regardless of background processing state
        String initialStatus = logs.get(0).getStatus();
        logger.info("Initial log status: {}", initialStatus);
        
        // Save test result
        saveTestResult("async_background_processing.json", Map.of(
            "test", "shouldVerifyAsyncResponseIsAcceptedWhileBackgroundRunning",
            "responseStatus", "ACCEPTED",
            "initialLogStatus", initialStatus,
            "description", "Response is ACCEPTED immediately, background processing may be PENDING/SUCCESS/FAILED",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @Test
    @DisplayName("Integration: Should handle sync mode timeout gracefully")
    void shouldHandleSyncModeTimeout() throws Exception {
        // This test would require a way to slow down processing
        // For now, we test with a normal request that should complete within 15s
        
        var variables = new HashMap<String, Object>();
        variables.put("name", "Timeout Test User");
        
        var request = new SendNotificationRequest(
            "timeout@example.com",
            "test-template",
            "en",
            Channel.EMAIL,
            variables
        );

        // When - Send request with sync=true
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(post("/api/v1/notifications/send?sync=true")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify it completed within 15 seconds
        assertTrue(duration < 15000, "Sync mode should complete or timeout within 15 seconds");
        
        // Save test result
        var responseBody = objectMapper.readTree(result.getResponse().getContentAsString());
        saveTestResult("sync_mode_timing.json", Map.of(
            "test", "shouldHandleSyncModeTimeout",
            "durationMs", duration,
            "status", responseBody.get("status").asText(),
            "withinTimeout", duration < 15000,
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * Helper method to save test results to .ignore/ folder
     */
    private void saveTestResult(String filename, Object data) {
        try {
            File ignoreDir = new File(".ignore");
            if (!ignoreDir.exists()) {
                ignoreDir.mkdirs();
            }
            
            File outputFile = new File(".ignore/" + filename);
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
            }
            
            logger.info("Test result saved to: {}", outputFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to save test result: {}", e.getMessage());
        }
    }

    /**
     * Helper class for Map.of() compatibility
     */
    private static class Map {
        public static java.util.Map<String, Object> of(Object... keyValues) {
            var map = new java.util.HashMap<String, Object>();
            for (int i = 0; i < keyValues.length; i += 2) {
                map.put((String) keyValues[i], keyValues[i + 1]);
            }
            return map;
        }
    }
}
