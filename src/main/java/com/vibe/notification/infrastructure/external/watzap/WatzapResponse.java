package com.vibe.notification.infrastructure.external.watzap;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Watzap API response models
 */
public record WatzapResponse(
    @JsonProperty("status")
    String status,

    @JsonProperty("message")
    String message,

    @JsonProperty("ack")
    String ack
) {
    public boolean isSuccess() {
        return "200".equals(status) || "successfully".equalsIgnoreCase(ack);
    }
}
