package com.vibe.notification.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TemplateType {
    TEXT,
    IMAGE;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static TemplateType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Template type cannot be null or empty");
        }

        return switch (value.toUpperCase()) {
            case "TEXT" -> TEXT;
            case "IMAGE" -> IMAGE;
            default -> throw new IllegalArgumentException("Invalid template type: " + value);
        };
    }

    public static TemplateType from(String value) {
        return fromString(value);
    }
}
