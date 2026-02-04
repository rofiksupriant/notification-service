# Async-Default API with Optional Sync Mode

## Overview
The Notification Service API now supports both asynchronous (default) and synchronous modes for sending notifications.

## API Usage

### Default Behavior (Async Mode)
```bash
POST /api/v1/notifications/send
Content-Type: application/json

{
  "recipient": "user@example.com",
  "slug": "welcome",
  "language": "en",
  "channel": "EMAIL",
  "variables": {
    "name": "John Doe"
  }
}
```

**Response (HTTP 202 Accepted):**
```json
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "ACCEPTED",
  "message": "Notification accepted for processing",
  "providerStatus": null
}
```

### Synchronous Mode (with ?sync=true)
```bash
POST /api/v1/notifications/send?sync=true
Content-Type: application/json

{
  "recipient": "user@example.com",
  "slug": "welcome",
  "language": "en",
  "channel": "EMAIL",
  "variables": {
    "name": "John Doe"
  }
}
```

**Response (HTTP 200 OK):**
```json
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "SUCCESS",
  "message": "Notification sent successfully",
  "providerStatus": "SUCCESS"
}
```

## Key Features

### 1. Async Mode (Default)
- Returns **202 Accepted** immediately with a trace_id
- Background processing happens asynchronously
- Ideal for high-throughput scenarios
- Client can use trace_id to track status later

### 2. Sync Mode (Optional)
- Triggered via `?sync=true` query parameter
- Waits for provider response (max 15 seconds timeout)
- Returns **200 OK** with final status (SUCCESS/FAILED)
- Includes `providerStatus` field in response
- Useful when immediate confirmation is needed

### 3. Common Features
- Both modes trigger RabbitMQ Status Callback after completion
- trace_id is propagated consistently across threads
- Supports idempotency via `Idempotency-Key` header
- DDD pattern maintained throughout implementation

## Technical Implementation

### Architecture
```
Controller (Presentation Layer)
    ↓
NotificationApplicationService (Application Layer)
    ↓
NotificationDomainService (Domain Layer)
    ↓
Ports/Adapters (Infrastructure Layer)
```

### Async Processing
- Uses Spring `@Async` with managed `TaskExecutor`
- `CompletableFuture` bridges sync and async modes
- Trace context propagated via custom decorator
- Maximum wait time: 15 seconds for sync mode

### Response Status Codes
| Mode  | Success Code | Status Field | Provider Status |
|-------|--------------|--------------|-----------------|
| Async | 202 Accepted | "ACCEPTED"   | null            |
| Sync  | 200 OK       | "SUCCESS" or "FAILED" | NotificationStatus enum |

## Testing

### Unit Tests
- Verify async mode returns 202 immediately
- Verify sync mode returns 200 with final status
- Mock domain service for controlled testing
- Test timeout handling

### Integration Tests  
- Verify complete async flow with database
- Verify sync response contains provider status
- Verify trace_id consistency across threads
- Verify async returns ACCEPTED even if background running

### Test Results Location
All test results and reports are stored in `.ignore/` folder:
- `async_mode_response_time.json`
- `sync_mode_final_status.json`
- `trace_id_consistency.json`
- `async_background_processing.json`
- `sync_mode_timing.json`

## Examples

### Async Mode (Fire-and-Forget)
```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "user@example.com",
    "slug": "order_confirmation",
    "language": "en",
    "channel": "EMAIL",
    "variables": {
      "orderId": "ORD-12345",
      "amount": "Rp 1.000.000"
    }
  }'
```

### Sync Mode (Wait for Result)
```bash
curl -X POST "http://localhost:8080/api/v1/notifications/send?sync=true" \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "+6281234567890",
    "slug": "otp",
    "language": "en",
    "channel": "WHATSAPP",
    "variables": {
      "otp": "123456"
    }
  }'
```

### With Idempotency Key
```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: unique-request-id-123" \
  -d '{
    "recipient": "user@example.com",
    "slug": "welcome",
    "language": "en",
    "channel": "EMAIL",
    "variables": {
      "name": "Jane Doe"
    }
  }'
```

## Error Handling

### Timeout (Sync Mode Only)
If processing takes longer than 15 seconds:
```json
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "TIMEOUT",
  "message": "Notification processing timed out after 15 seconds",
  "providerStatus": null
}
```

### Processing Failure
```json
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "FAILED",
  "message": "Notification failed: Template not found",
  "providerStatus": "FAILED"
}
```

## Best Practices

1. **Use Async Mode for Bulk Operations**: Default async mode is ideal for sending multiple notifications
2. **Use Sync Mode Sparingly**: Only use sync mode when you need immediate confirmation (e.g., OTP verification)
3. **Implement Retry Logic**: For critical notifications, implement retry logic on the client side
4. **Monitor Timeouts**: Track timeout rates for sync mode to optimize performance
5. **Use Idempotency Keys**: Always use idempotency keys for critical operations to prevent duplicates

## Performance Considerations

- **Async Mode**: Near-instant response (~10ms), suitable for high-throughput
- **Sync Mode**: Response time depends on provider (~50ms to 15s)
- **Thread Pool**: Managed by Spring's async executor configuration
- **Database**: All notifications are logged regardless of mode
