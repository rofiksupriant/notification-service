package com.vibe.notification.infrastructure.adapter.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component for validating API keys in requests
 * Implements shared secret validation for API authorization
 */
@Component
public class ApiKeyValidator {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyValidator.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    @Value("${api.key.secret:default-dev-key}")
    private String apiKeySecret;

    /**
     * Validate the API key from request header
     *
     * @param apiKey the API key from header
     * @return true if valid, false otherwise
     */
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("API key validation failed: API key is missing");
            return false;
        }

        boolean isValid = apiKey.equals(apiKeySecret);
        if (!isValid) {
            logger.warn("API key validation failed: Invalid API key provided");
        }
        return isValid;
    }

    /**
     * Get the API key header name
     */
    public static String getApiKeyHeader() {
        return API_KEY_HEADER;
    }
}
