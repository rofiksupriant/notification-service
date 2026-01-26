package com.vibe.notification.domain.model;

public enum TemplateType {
    TEXT,
    IMAGE;

    public static TemplateType from(String value) {
        return switch (value) {
            case "TEXT" -> TEXT;
            case "IMAGE" -> IMAGE;
            default -> throw new IllegalArgumentException("Invalid template type: " + value);
        };
    }
}
