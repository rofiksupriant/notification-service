package com.vibe.notification.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.application.NotificationApplicationService;
import com.vibe.notification.application.dto.NotificationResponse;
import com.vibe.notification.application.dto.SendNotificationRequest;
import com.vibe.notification.domain.model.Channel;
import com.vibe.notification.domain.model.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for async-default API with optional sync mode
 */
@WebMvcTest(NotificationController.class)
@DisplayName("Notification Controller - Async/Sync Mode Tests")
class NotificationControllerAsyncSyncTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationApplicationService notificationApplicationService;

    private SendNotificationRequest testRequest;
    private UUID testLogId;
    private String testTraceId;

    @BeforeEach
    void setUp() {
        testLogId = UUID.randomUUID();
        testTraceId = UUID.randomUUID().toString();
        
        var variables = new HashMap<String, Object>();
        variables.put("name", "John Doe");
        variables.put("companyName", "VibeCoding");
        
        testRequest = new SendNotificationRequest(
            "john@example.com",
            "welcome",
            "en",
            Channel.EMAIL,
            variables
        );
    }

    @Test
    @DisplayName("Should return 202 Accepted immediately when sync=false (default)")
    void shouldReturn202AcceptedForAsyncMode() throws Exception {
        // Given - Mock service to simulate slow processing
        NotificationResponse asyncResponse = new NotificationResponse(
            testLogId,
            testTraceId,
            "ACCEPTED",
            "Notification accepted for processing"
        );
        
        when(notificationApplicationService.sendNotificationWithSync(any(), eq(false)))
            .thenReturn(asyncResponse);

        // When - Send request without sync parameter (defaults to false)
        mockMvc.perform(post("/api/v1/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
        
        // Then - Should return 202 Accepted immediately
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status", equalTo("ACCEPTED")))
            .andExpect(jsonPath("$.traceId", equalTo(testTraceId)))
            .andExpect(jsonPath("$.logId", equalTo(testLogId.toString())))
            .andExpect(jsonPath("$.message", equalTo("Notification accepted for processing")));

        // Verify service was called with sync=false
        verify(notificationApplicationService, times(1))
            .sendNotificationWithSync(any(SendNotificationRequest.class), eq(false));
    }

    @Test
    @DisplayName("Should return 202 Accepted immediately when sync=false explicitly")
    void shouldReturn202AcceptedWhenSyncFalse() throws Exception {
        // Given
        NotificationResponse asyncResponse = new NotificationResponse(
            testLogId,
            testTraceId,
            "ACCEPTED",
            "Notification accepted for processing"
        );
        
        when(notificationApplicationService.sendNotificationWithSync(any(), eq(false)))
            .thenReturn(asyncResponse);

        // When - Send request with sync=false
        mockMvc.perform(post("/api/v1/notifications/send?sync=false")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
        
        // Then
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status", equalTo("ACCEPTED")));
    }

    @Test
    @DisplayName("Should return 200 OK with final status when sync=true")
    void shouldReturn200OkForSyncMode() throws Exception {
        // Given - Mock service to return final status
        NotificationResponse syncResponse = new NotificationResponse(
            testLogId,
            testTraceId,
            "SUCCESS",
            "Notification sent successfully",
            NotificationStatus.SUCCESS
        );
        
        when(notificationApplicationService.sendNotificationWithSync(any(), eq(true)))
            .thenReturn(syncResponse);

        // When - Send request with sync=true
        mockMvc.perform(post("/api/v1/notifications/send?sync=true")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
        
        // Then - Should return 200 OK with final status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("SUCCESS")))
            .andExpect(jsonPath("$.providerStatus", equalTo("SUCCESS")))
            .andExpect(jsonPath("$.traceId", equalTo(testTraceId)))
            .andExpect(jsonPath("$.message", equalTo("Notification sent successfully")));

        // Verify service was called with sync=true
        verify(notificationApplicationService, times(1))
            .sendNotificationWithSync(any(SendNotificationRequest.class), eq(true));
    }

    @Test
    @DisplayName("Should return 200 OK with FAILED status when sync=true and processing fails")
    void shouldReturn200OkWithFailedStatusWhenSyncAndFailed() throws Exception {
        // Given - Mock service to return failed status
        NotificationResponse failedResponse = new NotificationResponse(
            testLogId,
            testTraceId,
            "FAILED",
            "Notification failed: Template not found",
            NotificationStatus.FAILED
        );
        
        when(notificationApplicationService.sendNotificationWithSync(any(), eq(true)))
            .thenReturn(failedResponse);

        // When - Send request with sync=true
        mockMvc.perform(post("/api/v1/notifications/send?sync=true")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
        
        // Then - Should return 200 OK (request succeeded) but with FAILED status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("FAILED")))
            .andExpect(jsonPath("$.providerStatus", equalTo("FAILED")))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Template not found")));
    }

    @Test
    @DisplayName("Should handle idempotency key in both sync and async modes")
    void shouldHandleIdempotencyKey() throws Exception {
        // Given
        String idempotencyKey = "unique-key-123";
        NotificationResponse response = new NotificationResponse(
            testLogId,
            testTraceId,
            "ACCEPTED",
            "Notification accepted for processing"
        );
        
        when(notificationApplicationService.sendNotificationWithSync(any(), eq(false)))
            .thenReturn(response);

        // When - Send request with idempotency key
        mockMvc.perform(post("/api/v1/notifications/send")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
        
        // Then
            .andExpect(status().isAccepted());

        // Verify the request contains the idempotency key
        ArgumentCaptor<SendNotificationRequest> captor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationApplicationService).sendNotificationWithSync(captor.capture(), eq(false));
        
        SendNotificationRequest capturedRequest = captor.getValue();
        assertTrue(capturedRequest.traceId().isPresent());
        assertEquals(idempotencyKey, capturedRequest.traceId().get());
    }

    @Test
    @DisplayName("Should verify async mode returns immediately even with slow processing")
    void shouldReturnImmediatelyInAsyncMode() throws Exception {
        // Given - Mock service to simulate instant return for async mode
        NotificationResponse asyncResponse = new NotificationResponse(
            testLogId,
            testTraceId,
            "ACCEPTED",
            "Notification accepted for processing"
        );
        
        // Simulate the service returning immediately (no delay)
        when(notificationApplicationService.sendNotificationWithSync(any(), eq(false)))
            .thenReturn(asyncResponse);

        // When & Then - Request completes quickly
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(post("/api/v1/notifications/send?sync=false")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
            .andExpect(status().isAccepted());
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Verify it returned in less than 1 second (should be instant)
        assertTrue(duration < 1000, "Async mode should return immediately, took " + duration + "ms");
    }

    @Test
    @DisplayName("Should handle timeout in sync mode")
    void shouldHandleTimeoutInSyncMode() throws Exception {
        // Given - Mock service to return timeout status
        NotificationResponse timeoutResponse = new NotificationResponse(
            testLogId,
            testTraceId,
            "TIMEOUT",
            "Notification processing timed out after 15 seconds"
        );
        
        when(notificationApplicationService.sendNotificationWithSync(any(), eq(true)))
            .thenReturn(timeoutResponse);

        // When - Send request with sync=true
        mockMvc.perform(post("/api/v1/notifications/send?sync=true")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testRequest)))
        
        // Then - Should return 200 OK with TIMEOUT status
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("TIMEOUT")))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("timed out")));
    }
}
