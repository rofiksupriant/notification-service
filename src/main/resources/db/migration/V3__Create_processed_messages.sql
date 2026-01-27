-- Migration: Create processed_messages table for idempotent RabbitMQ message processing
-- Purpose: Tracks trace IDs of successfully processed messages to prevent duplicate handling

CREATE TABLE IF NOT EXISTS processed_messages (
    trace_id VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_messages_created_at ON processed_messages(created_at);
