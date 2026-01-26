package com.vibe.notification.infrastructure.external.watzap;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Watzap API response models
 */
public record WatzapResponse(
    @JsonProperty("success")
    boolean success,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("data")
    WatzapData data
) {
    public record WatzapData(
        @JsonProperty("id")
        String id,
        
        @JsonProperty("status")
        String status
    ) {}
}
