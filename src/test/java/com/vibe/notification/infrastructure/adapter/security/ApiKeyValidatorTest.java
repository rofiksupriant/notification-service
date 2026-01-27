package com.vibe.notification.infrastructure.adapter.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("API Key Validator Tests")
class ApiKeyValidatorTest {

    private ApiKeyValidator apiKeyValidator;

    @BeforeEach
    void setUp() {
        apiKeyValidator = new ApiKeyValidator();
        // Set the secret via reflection
        ReflectionTestUtils.setField(apiKeyValidator, "apiKeySecret", "test-secret-key");
    }

    @Test
    @DisplayName("Should validate correct API key")
    void shouldValidateCorrectApiKey() {
        // When
        boolean result = apiKeyValidator.validateApiKey("test-secret-key");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject incorrect API key")
    void shouldRejectIncorrectApiKey() {
        // When
        boolean result = apiKeyValidator.validateApiKey("wrong-key");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject null API key")
    void shouldRejectNullApiKey() {
        // When
        boolean result = apiKeyValidator.validateApiKey(null);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject empty API key")
    void shouldRejectEmptyApiKey() {
        // When
        boolean result = apiKeyValidator.validateApiKey("");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject blank API key")
    void shouldRejectBlankApiKey() {
        // When
        boolean result = apiKeyValidator.validateApiKey("   ");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return correct API key header name")
    void shouldReturnCorrectApiKeyHeaderName() {
        // When
        String headerName = ApiKeyValidator.getApiKeyHeader();

        // Then
        assertEquals("X-API-Key", headerName);
    }
}
