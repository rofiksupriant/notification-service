package com.vibe.notification.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vibe.notification.application.dto.CreateTemplateRequest;
import com.vibe.notification.application.dto.TemplateResponse;
import com.vibe.notification.application.dto.UpdateTemplateRequest;
import com.vibe.notification.domain.exception.TemplateAlreadyExistsException;
import com.vibe.notification.domain.exception.TemplateNotFoundException;
import com.vibe.notification.infrastructure.adapter.TemplateManagementAdapter;
import com.vibe.notification.infrastructure.adapter.security.ApiKeyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TemplateController.class)
@DisplayName("Template Controller Tests")
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TemplateManagementAdapter templateManagementAdapter;

    @MockitoBean
    private ApiKeyValidator apiKeyValidator;

    private TemplateResponse templateResponse;
    private CreateTemplateRequest createRequest;
    private UpdateTemplateRequest updateRequest;
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        templateResponse = new TemplateResponse(
            "welcome",
            "en",
            "EMAIL",
            "TEXT",
            "Welcome",
            "Hello [[${name}]]",
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        createRequest = new CreateTemplateRequest(
            "welcome",
            "en",
            "EMAIL",
            "Hello [[${name}]]",
            "Welcome",
            null,
            "TEXT"
        );

        updateRequest = new UpdateTemplateRequest(
            "Updated content",
            "Updated Subject",
            null
        );
    }

    @Test
    @DisplayName("POST /api/v1/templates - Should create template with valid API key")
    void shouldCreateTemplateWithValidApiKey() throws Exception {
        // Given
        when(apiKeyValidator.validateApiKey(VALID_API_KEY)).thenReturn(true);
        when(templateManagementAdapter.createTemplate(any())).thenReturn(templateResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/templates")
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("welcome"))
            .andExpect(jsonPath("$.language").value("en"))
            .andExpect(jsonPath("$.channel").value("EMAIL"));

        verify(apiKeyValidator).validateApiKey(VALID_API_KEY);
        verify(templateManagementAdapter).createTemplate(any());
    }

    @Test
    @DisplayName("POST /api/v1/templates - Should reject request with invalid API key")
    void shouldRejectCreateTemplateWithInvalidApiKey() throws Exception {
        // Given
        when(apiKeyValidator.validateApiKey("invalid-key")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/templates")
                .header(API_KEY_HEADER, "invalid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isUnauthorized());

        verify(templateManagementAdapter, never()).createTemplate(any());
    }

    @Test
    @DisplayName("GET /api/v1/templates/{slug}/{lang} - Should fetch template successfully")
    void shouldFetchTemplateSuccessfully() throws Exception {
        // Given
        when(templateManagementAdapter.getTemplate("welcome", "en")).thenReturn(templateResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/templates/welcome/en")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("welcome"))
            .andExpect(jsonPath("$.language").value("en"))
            .andExpect(jsonPath("$.channel").value("EMAIL"));

        verify(templateManagementAdapter).getTemplate("welcome", "en");
    }

    @Test
    @DisplayName("GET /api/v1/templates/{slug}/{lang} - Should return 404 when template not found")
    void shouldReturn404WhenTemplateNotFound() throws Exception {
        // Given
        when(templateManagementAdapter.getTemplate("missing", "en"))
            .thenThrow(new TemplateNotFoundException("missing", "en"));

        // When & Then
        mockMvc.perform(get("/api/v1/templates/missing/en")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/templates/{slug}/{lang} - Should update template with valid API key")
    void shouldUpdateTemplateWithValidApiKey() throws Exception {
        // Given
        when(apiKeyValidator.validateApiKey(VALID_API_KEY)).thenReturn(true);
        when(templateManagementAdapter.updateTemplate("welcome", "en", updateRequest))
            .thenReturn(templateResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/templates/welcome/en")
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("welcome"))
            .andExpect(jsonPath("$.language").value("en"));

        verify(apiKeyValidator).validateApiKey(VALID_API_KEY);
        verify(templateManagementAdapter).updateTemplate("welcome", "en", updateRequest);
    }

    @Test
    @DisplayName("PUT /api/v1/templates/{slug}/{lang} - Should reject update with invalid API key")
    void shouldRejectUpdateTemplateWithInvalidApiKey() throws Exception {
        // Given
        when(apiKeyValidator.validateApiKey("invalid-key")).thenReturn(false);

        // When & Then
        mockMvc.perform(put("/api/v1/templates/welcome/en")
                .header(API_KEY_HEADER, "invalid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isUnauthorized());

        verify(templateManagementAdapter, never()).updateTemplate(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/templates/{slug}/{lang} - Should delete template with valid API key")
    void shouldDeleteTemplateWithValidApiKey() throws Exception {
        // Given
        when(apiKeyValidator.validateApiKey(VALID_API_KEY)).thenReturn(true);
        doNothing().when(templateManagementAdapter).deleteTemplate("welcome", "en");

        // When & Then
        mockMvc.perform(delete("/api/v1/templates/welcome/en")
                .header(API_KEY_HEADER, VALID_API_KEY))
            .andExpect(status().isNoContent());

        verify(apiKeyValidator).validateApiKey(VALID_API_KEY);
        verify(templateManagementAdapter).deleteTemplate("welcome", "en");
    }

    @Test
    @DisplayName("DELETE /api/v1/templates/{slug}/{lang} - Should reject delete with invalid API key")
    void shouldRejectDeleteTemplateWithInvalidApiKey() throws Exception {
        // Given
        when(apiKeyValidator.validateApiKey("invalid-key")).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/v1/templates/welcome/en")
                .header(API_KEY_HEADER, "invalid-key"))
            .andExpect(status().isUnauthorized());

        verify(templateManagementAdapter, never()).deleteTemplate(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/v1/templates - Should return 409 when template already exists")
    void shouldReturn409WhenTemplateAlreadyExists() throws Exception {
        // Given
        when(apiKeyValidator.validateApiKey(VALID_API_KEY)).thenReturn(true);
        when(templateManagementAdapter.createTemplate(any()))
            .thenThrow(new TemplateAlreadyExistsException("welcome", "en"));

        // When & Then
        mockMvc.perform(post("/api/v1/templates")
                .header(API_KEY_HEADER, VALID_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isConflict());
    }
}
