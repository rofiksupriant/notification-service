package com.vibe.notification.application.port;

/**
 * Port for idempotency checks (processed messages).
 * Application layer defines the contract; infrastructure implements it.
 */
public interface IdempotencyPort {
    boolean isMessageAlreadyProcessed(String messageId);
    void markMessageAsProcessed(String messageId);
}
