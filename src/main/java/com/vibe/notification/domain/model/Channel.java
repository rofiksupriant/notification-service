package com.vibe.notification.domain.model;

public enum Channel {
    EMAIL,
    WHATSAPP;

    public static Channel from(String value) {
        return switch (value) {
            case "EMAIL" -> EMAIL;
            case "WHATSAPP" -> WHATSAPP;
            default -> throw new IllegalArgumentException("Invalid channel: " + value);
        };
    }
}
