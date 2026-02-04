package com.vibe.notification.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationStatus {
    PENDING,
    SUCCESS,
    FAILED,
    RETRY_EXHAUSTED;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static NotificationStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Notification status cannot be null or empty");
        }

        return switch (value.toUpperCase()) {
            case "PENDING" -> PENDING;
            case "SUCCESS" -> SUCCESS;
            case "FAILED" -> FAILED;
            case "RETRY_EXHAUSTED" -> RETRY_EXHAUSTED;
            default -> throw new IllegalArgumentException("Invalid status: " + value);
        };
    }

    public static NotificationStatus from(String value) {
        return fromString(value);
    }
}
