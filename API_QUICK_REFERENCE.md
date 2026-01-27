# Quick Reference - Notification Service API

## 📡 Send Notification via HTTP API

### Basic Request
```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "recipient": "+6281234567890",
    "slug": "welcome",
    "language": "en",
    "channel": "whatsapp",
    "variables": {"userName": "John"}
  }'
```

### With Idempotency (Trace ID)
```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -H "X-Trace-ID: unique-request-id" \
  -d '{
    "recipient": "user@example.com",
    "slug": "welcome",
    "language": "en",
    "channel": "email",
    "variables": {"userName": "John"}
  }'
```

## 📨 Send via RabbitMQ

### Python
```python
import pika, json, uuid

conn = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
ch = conn.channel()
ch.queue_declare(queue='notification.request.queue', durable=True)

msg = {
    "traceId": str(uuid.uuid4()),
    "recipient": "+6281234567890",
    "slug": "welcome",
    "language": "en",
    "channel": "whatsapp",
    "variables": {"userName": "John"}
}

ch.basic_publish(
    exchange='', routing_key='notification.request.queue',
    body=json.dumps(msg),
    properties=pika.BasicProperties(content_type='application/json', persistent=True)
)
conn.close()
```

### Node.js
```javascript
const amqp = require('amqplib');
const {v4: uuidv4} = require('uuid');

(async () => {
    const conn = await amqp.connect('amqp://guest:guest@localhost');
    const ch = await conn.createChannel();
    await ch.assertQueue('notification.request.queue', {durable: true});
    
    const msg = {
        traceId: uuidv4(),
        recipient: '+6281234567890',
        slug: 'welcome',
        language: 'en',
        channel: 'whatsapp',
        variables: {userName: 'John'}
    };
    
    ch.sendToQueue('notification.request.queue', Buffer.from(JSON.stringify(msg)), {
        contentType: 'application/json', persistent: true
    });
    
    await ch.close();
    await conn.close();
})();
```

### Java
```java
@Service
public class NotificationPublisher {
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private ObjectMapper mapper;
    
    public void send(String recipient, String slug, String language, 
                     String channel, Map<String, Object> vars) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("traceId", UUID.randomUUID());
        msg.put("recipient", recipient);
        msg.put("slug", slug);
        msg.put("language", language);
        msg.put("channel", channel);
        msg.put("variables", vars);
        rabbitTemplate.convertAndSend("notification.request.queue", 
                                     mapper.writeValueAsString(msg));
    }
}
```

### Go
```go
import "github.com/rabbitmq/amqp091-go"

conn, _ := amqp.Dial("amqp://guest:guest@localhost")
ch, _ := conn.Channel()
ch.QueueDeclare("notification.request.queue", true, false, false, false, nil)

msg := map[string]interface{}{
    "traceId": uuid.New(),
    "recipient": "+6281234567890",
    "slug": "welcome",
    "language": "en",
    "channel": "whatsapp",
    "variables": map[string]interface{}{"userName": "John"},
}

data, _ := json.Marshal(msg)
ch.Publish("", "notification.request.queue", false, false, 
    amqp.Publishing{ContentType: "application/json", Body: data})
```

## 🔑 Required Fields

| Field | Type | Example | Notes |
|-------|------|---------|-------|
| `recipient` | string | `+6281234567890` or `user@example.com` | Phone for WhatsApp, email for Email |
| `slug` | string | `welcome` | Template identifier in database |
| `language` | string | `en`, `id`, `es` | ISO 639-1 code |
| `channel` | string | `whatsapp` or `email` | Delivery channel |
| `variables` | object | `{"userName": "John"}` | Optional template variables |
| `traceId` | string | `uuid-here` | Optional, for idempotency |

## 📊 Supported Channels

### WhatsApp (Watzap.id)
```json
{
  "recipient": "+6281234567890",
  "slug": "otp",
  "language": "en",
  "channel": "whatsapp",
  "variables": {"otp_code": "123456"}
}
```

### Email
```json
{
  "recipient": "user@example.com",
  "slug": "welcome",
  "language": "en",
  "channel": "email",
  "variables": {"userName": "John", "link": "https://..."}
}
```

## ✅ Response Codes

| Code | Meaning |
|------|---------|
| `202` | Accepted - Processing in background |
| `400` | Bad Request - Missing/invalid field |
| `401` | Unauthorized - Invalid API key |
| `404` | Not Found - Template not found |
| `500` | Server Error - Unexpected failure |

## 🔍 Check Notification Status

```bash
curl http://localhost:8080/api/v1/notifications/logs/{logId} \
  -H "X-API-Key: your-api-key"
```

**Response:**
```json
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "msg-123",
  "status": "SUCCESS",
  "processedAt": "2024-01-01T12:00:00Z",
  "errorMessage": null
}
```

## 🐳 Docker Commands

```bash
# Start all services
docker-compose up -d

# Check RabbitMQ UI
open http://localhost:15672  # guest/guest

# View logs
docker logs -f notification-service

# Stop services
docker-compose down
```

## 🧪 Run Tests

```bash
# All tests
mvn test

# HTTP API tests
mvn test -Dtest=NotificationIntegrationTest

# RabbitMQ tests
mvn test -Dtest=RabbitMqIntegrationTest

# With coverage
mvn clean test jacoco:report
```

## 📖 Full Documentation

See [API_GUIDE.md](API_GUIDE.md) for complete guide with:
- Detailed examples for each language
- Error handling & retry strategies
- Configuration options
- Monitoring & debugging
- Architecture overview

