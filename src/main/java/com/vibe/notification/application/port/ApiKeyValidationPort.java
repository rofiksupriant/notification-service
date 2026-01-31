package com.vibe.notification.application.port;

/**
 * Port interface for API key validation
 * Defines the contract for security/authentication operations
 */
public interface ApiKeyValidationPort {
    /**
     * Validate an API key
     *
     * @param apiKey the API key to validate
     * @return true if valid, false otherwise
     */
    boolean validateApiKey(String apiKey);
}
