CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    trace_id UUID NOT NULL,
    recipient VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    variables JSONB,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_logs_trace_id ON notification_logs(trace_id);
CREATE INDEX IF NOT EXISTS idx_logs_recipient ON notification_logs(recipient);
CREATE INDEX IF NOT EXISTS idx_logs_status ON notification_logs(status);
CREATE INDEX IF NOT EXISTS idx_logs_created_at ON notification_logs(created_at);
