package com.vibe.notification.domain.model;

/**
 * Domain model representing the result of a notification processing
 * 
 * @param status the final status (SUCCESS or FAILED)
 * @param errorMessage optional error message if failed
 */
public record NotificationResult(
    NotificationStatus status,
    String errorMessage
) {
    /**
     * Create a successful result
     */
    public static NotificationResult success() {
        return new NotificationResult(NotificationStatus.SUCCESS, null);
    }
    
    /**
     * Create a failed result with error message
     */
    public static NotificationResult failure(String errorMessage) {
        return new NotificationResult(NotificationStatus.FAILED, errorMessage);
    }
    
    /**
     * Check if the notification was successful
     */
    public boolean isSuccess() {
        return status == NotificationStatus.SUCCESS;
    }
}
