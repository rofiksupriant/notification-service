package com.vibe.notification.domain.model;

public enum NotificationStatus {
    PENDING,
    SUCCESS,
    FAILED;

    public static NotificationStatus from(String value) {
        return switch (value) {
            case "PENDING" -> PENDING;
            case "SUCCESS" -> SUCCESS;
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("Invalid status: " + value);
        };
    }
}
