# Notification Service - Complete API Guide

This guide provides complete instructions on how to use the Notification Service through both the **HTTP REST API** and the **RabbitMQ messaging channel**.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [HTTP REST API](#http-rest-api)
3. [RabbitMQ Channel](#rabbitmq-channel)
4. [Idempotency & Tracing](#idempotency--tracing)
5. [Examples](#examples)
6. [Error Handling](#error-handling)
7. [Testing](#testing)

---

## Quick Start

### Prerequisites

```bash
# Start services using Docker Compose
docker-compose up -d

# Expected services:
# - PostgreSQL: localhost:5432
# - RabbitMQ: localhost:5672 (Management UI: localhost:15672)
# - Notification Service: localhost:8080
```

### Health Check

```bash
# Verify service is running
curl http://localhost:8080/actuator/health
```

---

## HTTP REST API

### Base URL
```
http://localhost:8080
```

### Endpoint: Send Notification (Async)

**POST** `/api/v1/notifications/send`

#### Request Headers
```
Content-Type: application/json
X-Trace-ID: [optional-uuid]  # For idempotent processing
X-API-Key: [required]        # API key for authentication
```

#### Request Body
```json
{
  "recipient": "+6281234567890",        // Phone (WhatsApp) or email
  "slug": "welcome",                    // Template identifier
  "language": "en",                     // ISO 639-1 code (en, id, es, etc.)
  "channel": "whatsapp",                // "whatsapp" or "email"
  "variables": {                        // Template variables (optional)
    "userName": "John Doe",
    "activationLink": "https://example.com/activate"
  }
}
```

#### Response (202 Accepted)
```json
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "auto-generated-or-provided",
  "status": "PENDING",
  "message": "Notification queued for processing"
}
```

#### Status Codes
- **202 Accepted** - Request accepted for async processing
- **400 Bad Request** - Missing required fields or invalid data
- **401 Unauthorized** - Missing or invalid API key
- **404 Not Found** - Template not found for given slug/language
- **500 Internal Server Error** - Unexpected error

---

### Complete HTTP Examples

#### Example 1: Send WhatsApp OTP

```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -H "X-Trace-ID: otp-user-12345-$(date +%s)" \
  -d '{
    "recipient": "+6281234567890",
    "slug": "otp",
    "language": "en",
    "channel": "whatsapp",
    "variables": {
      "otp_code": "123456",
      "expiry_minutes": "5"
    }
  }'
```

**Response:**
```json
{
  "logId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "traceId": "otp-user-12345-1704067200",
  "status": "PENDING",
  "message": "Notification queued for processing"
}
```

#### Example 2: Send Welcome Email

```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -d '{
    "recipient": "john@example.com",
    "slug": "welcome",
    "language": "en",
    "channel": "email",
    "variables": {
      "userName": "John Doe",
      "activationLink": "https://app.example.com/activate?token=xyz"
    }
  }'
```

#### Example 3: Send with Idempotency (Duplicate Prevention)

```bash
# Send the same notification with same trace_id twice
# Only the first one will be processed (second is skipped)

TRACE_ID="invoice-user-5678-$(date +%s%N)"

# First request
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -H "X-Trace-ID: $TRACE_ID" \
  -d '{
    "recipient": "billing@example.com",
    "slug": "invoice",
    "language": "en",
    "channel": "email",
    "variables": {
      "invoiceNumber": "INV-2024-001",
      "amount": "$150.00"
    }
  }'

# Second request with same trace_id (will be rejected as duplicate)
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -H "X-Trace-ID: $TRACE_ID" \
  -d '{
    "recipient": "billing@example.com",
    "slug": "invoice",
    "language": "en",
    "channel": "email",
    "variables": {
      "invoiceNumber": "INV-2024-001",
      "amount": "$150.00"
    }
  }'
```

#### Example 4: Get Notification Status

```bash
curl -X GET http://localhost:8080/api/v1/notifications/logs/{logId} \
  -H "X-API-Key: your-api-key-here"
```

**Response:**
```json
{
  "logId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "traceId": "otp-user-12345-1704067200",
  "recipient": "+6281234567890",
  "slug": "otp",
  "language": "en",
  "channel": "whatsapp",
  "status": "SUCCESS",
  "processedAt": "2024-01-01T12:00:00Z",
  "errorMessage": null
}
```

---

## RabbitMQ Channel

### Setup

RabbitMQ must be enabled in `application.yml`:

```yaml
app:
  feature:
    rabbitmq:
      enabled: true

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### Message Format

Messages are sent to the **`notification.request.queue`** as JSON.

#### Message Structure
```json
{
  "traceId": "unique-message-id-for-idempotency",
  "recipient": "+6281234567890",
  "slug": "welcome",
  "language": "en",
  "channel": "whatsapp",
  "variables": {
    "userName": "John Doe"
  }
}
```

### Complete RabbitMQ Examples

#### Example 1: Using RabbitMQ CLI (rabbitmqctl)

```bash
# Connect to RabbitMQ container
docker exec -it notification-rabbitmq rabbitmqctl

# Publish message
rabbitmq-publish --vhost / --username guest --password guest \
  --exchange notification.request.exchange \
  --routing-key notification.request.* \
  '{
    "traceId": "msg-001-'$(date +%s%N)'",
    "recipient": "+6281234567890",
    "slug": "otp",
    "language": "en",
    "channel": "whatsapp",
    "variables": {
      "otp_code": "654321",
      "expiry_minutes": "10"
    }
  }'
```

#### Example 2: Using Python with pika

```python
import pika
import json
import uuid
from datetime import datetime

# Connect to RabbitMQ
connection = pika.BlockingConnection(
    pika.ConnectionParameters('localhost')
)
channel = connection.channel()

# Declare queue
channel.queue_declare(queue='notification.request.queue', durable=True)

# Create message
message = {
    "traceId": f"python-msg-{uuid.uuid4()}",
    "recipient": "+6281234567890",
    "slug": "welcome",
    "language": "en",
    "channel": "whatsapp",
    "variables": {
        "userName": "John Doe",
        "activationLink": "https://example.com/activate"
    }
}

# Send message
channel.basic_publish(
    exchange='',
    routing_key='notification.request.queue',
    body=json.dumps(message),
    properties=pika.BasicProperties(
        content_type='application/json',
        delivery_mode=pika.spec.PERSISTENT_DELIVERY_MODE
    )
)

print(f"Message sent with traceId: {message['traceId']}")
connection.close()
```

#### Example 3: Using Node.js with amqplib

```javascript
const amqp = require('amqplib');
const { v4: uuidv4 } = require('uuid');

async function sendNotification() {
    const connection = await amqp.connect('amqp://guest:guest@localhost');
    const channel = await connection.createChannel();
    
    const queue = 'notification.request.queue';
    await channel.assertQueue(queue, { durable: true });
    
    const message = {
        traceId: `node-msg-${uuidv4()}`,
        recipient: '+6281234567890',
        slug: 'welcome',
        language: 'en',
        channel: 'whatsapp',
        variables: {
            userName: 'John Doe',
            activationLink: 'https://example.com/activate'
        }
    };
    
    channel.sendToQueue(queue, Buffer.from(JSON.stringify(message)), {
        contentType: 'application/json',
        persistent: true
    });
    
    console.log(`Message sent with traceId: ${message.traceId}`);
    
    await channel.close();
    await connection.close();
}

sendNotification().catch(console.error);
```

#### Example 4: Using Java with Spring AMQP

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    public NotificationPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void sendNotification(String recipient, String slug, 
                                String language, String channel, 
                                Map<String, Object> variables) {
        Map<String, Object> message = new HashMap<>();
        message.put("traceId", UUID.randomUUID().toString());
        message.put("recipient", recipient);
        message.put("slug", slug);
        message.put("language", language);
        message.put("channel", channel);
        message.put("variables", variables);
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend("notification.request.queue", jsonMessage);
            System.out.println("Message sent: " + message.get("traceId"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### Example 5: Using Go with amqp091-go

```go
package main

import (
	"encoding/json"
	"fmt"
	"log"

	amqp "github.com/rabbitmq/amqp091-go"
	"github.com/google/uuid"
)

type NotificationMessage struct {
	TraceID   string                 `json:"traceId"`
	Recipient string                 `json:"recipient"`
	Slug      string                 `json:"slug"`
	Language  string                 `json:"language"`
	Channel   string                 `json:"channel"`
	Variables map[string]interface{} `json:"variables"`
}

func main() {
	conn, err := amqp.Dial("amqp://guest:guest@localhost:5672/")
	if err != nil {
		log.Fatalf("Failed to connect to RabbitMQ: %v", err)
	}
	defer conn.Close()

	ch, err := conn.Channel()
	if err != nil {
		log.Fatalf("Failed to open a channel: %v", err)
	}
	defer ch.Close()

	q, err := ch.QueueDeclare(
		"notification.request.queue",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("Failed to declare a queue: %v", err)
	}

	msg := NotificationMessage{
		TraceID:   uuid.New().String(),
		Recipient: "+6281234567890",
		Slug:      "welcome",
		Language:  "en",
		Channel:   "whatsapp",
		Variables: map[string]interface{}{
			"userName":       "John Doe",
			"activationLink": "https://example.com/activate",
		},
	}

	body, _ := json.Marshal(msg)

	err = ch.Publish(
		"",
		q.Name,
		false,
		false,
		amqp.Publishing{
			ContentType: "application/json",
			Body:        body,
		},
	)
	if err != nil {
		log.Fatalf("Failed to publish a message: %v", err)
	}

	fmt.Printf("Message sent with traceId: %s\n", msg.TraceID)
}
```

---

## Idempotency & Tracing

### What is Idempotency?

Idempotency ensures that sending the same notification multiple times produces the same result as sending it once. This prevents duplicate messages from being sent.

### How It Works

Both channels use the **`traceId`** to prevent duplicates:

1. **First Request** with `traceId: "msg-123"`
   - Message is processed
   - `traceId` is stored in `processed_messages` table
   - Notification is sent

2. **Second Request** with same `traceId: "msg-123"`
   - System checks if `traceId` exists in `processed_messages`
   - Message is **skipped** (not re-processed)
   - Same result as first request (idempotent)

### Trace ID Guidelines

```
Format: {domain}-{user-id}-{timestamp}-{random}

Examples:
  otp-user-12345-1704067200-a1b2c3d4
  invoice-customer-5678-1704067201-x9y8z7w6
  welcome-onboarding-9999-1704067202-m5n4o3p2
```

### Distributed Tracing with MDC

All operations are logged with the `traceId` in the MDC (Mapped Diagnostic Context):

```
2024-01-01T12:00:00 [thread] INFO  [traceId=msg-123] Notification sent successfully
2024-01-01T12:00:01 [thread] ERROR [traceId=msg-123] Failed to send: Template not found
```

---

## Examples

### Scenario 1: User Registration Flow

```
1. User registers → API sends welcome email
2. System sends WhatsApp OTP
3. User verifies → Triggers activation notification
```

**Step 1: Send Welcome Email**
```bash
TRACE_ID="welcome-user-$(uuidgen)"

curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -H "X-Trace-ID: $TRACE_ID" \
  -d '{
    "recipient": "newuser@example.com",
    "slug": "welcome",
    "language": "en",
    "channel": "email",
    "variables": {
      "userName": "New User",
      "activationLink": "https://app.example.com/activate?token=xyz"
    }
  }'
```

**Step 2: Send OTP via WhatsApp**
```bash
TRACE_ID="otp-user-$(uuidgen)"

curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -H "X-Trace-ID: $TRACE_ID" \
  -d '{
    "recipient": "+6281234567890",
    "slug": "otp",
    "language": "en",
    "channel": "whatsapp",
    "variables": {
      "otp_code": "123456",
      "expiry_minutes": "10"
    }
  }'
```

### Scenario 2: Order Confirmation with Fallback Language

```
User requests notification in French, but template only exists in English → Falls back to English
```

```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "recipient": "customer@example.com",
    "slug": "order_confirmation",
    "language": "fr",        # French requested
    "channel": "email",
    "variables": {
      "orderNumber": "ORD-2024-001",
      "amount": "€99.99",
      "estimatedDelivery": "2024-01-05"
    }
  }'

# Response: Uses English template (fallback) since French not available
```

### Scenario 3: Batch Processing via RabbitMQ

```
System needs to send 1000 notifications → Use RabbitMQ for async batch processing
```

**Python Script:**
```python
import pika
import json
import uuid

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()
channel.queue_declare(queue='notification.request.queue', durable=True)

users = [
    {"email": "user1@example.com", "name": "User 1"},
    {"email": "user2@example.com", "name": "User 2"},
    # ... 998 more users
]

for user in users:
    message = {
        "traceId": f"batch-{uuid.uuid4()}",
        "recipient": user["email"],
        "slug": "monthly_report",
        "language": "en",
        "channel": "email",
        "variables": {"userName": user["name"]}
    }
    
    channel.basic_publish(
        exchange='',
        routing_key='notification.request.queue',
        body=json.dumps(message),
        properties=pika.BasicProperties(
            content_type='application/json',
            persistent=True
        )
    )

connection.close()
print("1000 notifications queued for processing")
```

---

## Error Handling

### Common Error Responses

#### 400 Bad Request - Missing Required Field
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Recipient is required",
  "path": "/api/v1/notifications/send"
}
```

#### 401 Unauthorized - Invalid API Key
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid API key",
  "path": "/api/v1/notifications/send"
}
```

#### 404 Not Found - Template Missing
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Template not found for slug='unknown' and language='en'",
  "path": "/api/v1/notifications/send"
}
```

### Retry Strategy (for HTTP API)

```bash
#!/bin/bash

MAX_RETRIES=3
RETRY_DELAY=2

for i in {1..MAX_RETRIES}; do
  RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST http://localhost:8080/api/v1/notifications/send \
    -H "Content-Type: application/json" \
    -H "X-API-Key: your-api-key" \
    -H "X-Trace-ID: idempotent-request-id" \
    -d '{...}')
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
  BODY=$(echo "$RESPONSE" | head -n-1)
  
  if [[ $HTTP_CODE == "202" ]]; then
    echo "Success: $BODY"
    break
  elif [[ $i -lt $MAX_RETRIES ]]; then
    echo "Retry $i failed with code $HTTP_CODE. Retrying in $RETRY_DELAY seconds..."
    sleep $RETRY_DELAY
  else
    echo "Failed after $MAX_RETRIES retries: $BODY"
  fi
done
```

---

## Testing

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test Suite
```bash
# HTTP API tests
mvn test -Dtest=NotificationIntegrationTest

# RabbitMQ tests
mvn test -Dtest=RabbitMqIntegrationTest

# Template tests
mvn test -Dtest=TemplateRenderingServiceTest
```

### Test Coverage
```bash
mvn clean test jacoco:report
# Open target/site/jacoco/index.html in browser
```

### Integration Test with RabbitMQ

```bash
# Start RabbitMQ (if using Docker Compose)
docker-compose up -d rabbitmq

# Run RabbitMQ integration tests
mvn test -Dtest=RabbitMqIntegrationTest

# View test output
cat target/surefire-reports/TEST-com.vibe.notification.integration.RabbitMqIntegrationTest.xml
```

---

## Configuration

### Enable/Disable Channels

**application.yml**
```yaml
app:
  feature:
    rabbitmq:
      enabled: true      # Enable RabbitMQ channel

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### API Key Management

**application.yml**
```yaml
api:
  key:
    secret: "your-secret-api-key-here"
```

**Usage in requests:**
```bash
curl -H "X-API-Key: your-secret-api-key-here" \
  http://localhost:8080/api/v1/notifications/send
```

### Email Configuration

**application.yml**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
```

---

## Monitoring & Debugging

### Health Endpoint
```bash
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/mail
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### View Logs
```bash
# Container logs
docker logs notification-service

# Log file
tail -f logs/notification-service.log

# Filter by trace_id
grep "traceId=msg-123" logs/notification-service.log
```

### Check Notification Status in Database

```sql
-- PostgreSQL
SELECT * FROM notification_logs 
WHERE trace_id = 'your-trace-id' 
ORDER BY created_at DESC;

-- Check processed messages (idempotency)
SELECT * FROM processed_messages 
WHERE trace_id = 'your-trace-id';
```

---

## Summary

| Feature | HTTP API | RabbitMQ |
|---------|----------|----------|
| **Synchronous** | ✅ | ❌ |
| **Asynchronous** | ✅ (Fire & Forget) | ✅ |
| **Idempotency** | ✅ (via X-Trace-ID header) | ✅ (via traceId field) |
| **Real-time Response** | ✅ | ❌ |
| **Batch Processing** | ⚠️ (one at a time) | ✅ (efficient) |
| **Authentication** | ✅ (API Key) | ⚠️ (connection-level) |
| **Rate Limiting** | ⚠️ (not implemented) | ❌ (none) |
| **Persistence** | ✅ (DB stored immediately) | ✅ (message queue) |

Choose **HTTP API** for real-time individual notifications with immediate response.
Choose **RabbitMQ** for batch processing and decoupled async workflows.

---

## Support & Documentation

For more information, see:
- [README.md](README.md) - Project overview
- [QUICKSTART.md](QUICKSTART.md) - Quick setup guide
- [API Documentation](http://localhost:8080/swagger-ui.html) - Swagger/OpenAPI docs
- [RabbitMQ Management](http://localhost:15672) - Admin console (guest/guest)

