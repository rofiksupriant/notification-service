package com.vibe.notification.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Channel {
    EMAIL,
    WHATSAPP;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static Channel fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Channel cannot be null or empty");
        }
        
        return switch (value.toUpperCase()) {
            case "EMAIL" -> EMAIL;
            case "WHATSAPP" -> WHATSAPP;
            default -> throw new IllegalArgumentException("Invalid channel: " + value);
        };
    }

    public static Channel from(String value) {
        return fromString(value);
    }
}
