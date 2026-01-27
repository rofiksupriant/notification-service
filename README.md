# Notification Service

A high-performance, asynchronous notification service built with Spring Boot 3.4+ and Java 25, designed to centralize WhatsApp (Watzap.id) and Email communications.

## 🏗️ Architecture

This project follows **Domain-Driven Design (DDD)** with clear separation of concerns:

```
Domain Layer (Business Logic)
    ↓
Application Layer (Use Cases & Orchestration)
    ↓
Infrastructure Layer (Data Access, External APIs)
    ↓
Presentation Layer (REST API)
```

## 🛠️ Technology Stack

- **Java 25**: Records, Pattern Matching, Unnamed Variables
- **Spring Boot 3.4+**: Web, Data JPA, Mail, Actuator, WebFlux
- **PostgreSQL 16**: JSONB support for flexible variable storage
- **Thymeleaf**: String Template Resolver for DB-based templates
- **Testcontainers**: PostgreSQL container for integration testing
- **Flyway**: Database migration management
- **Jackson**: JSON processing

## 🚀 Features

### Core Capabilities
- ✅ **Async Processing**: Non-blocking notification delivery with @Async
- ✅ **Template Engine**: Thymeleaf-based rendering with placeholder substitution
- ✅ **Language Fallback**: Automatic fallback from requested language → English
- ✅ **Distributed Tracing**: MDC-based trace_id for request tracking
- ✅ **Audit Trail**: Complete notification log with PENDING/SUCCESS/FAILED status
- ✅ **Multi-Channel**: Email (JavaMail) + WhatsApp (Watzap.id) support
- ✅ **Timeout Management**: 5s connect, 10s read timeouts on external APIs
- ✅ **Health Checks**: Custom actuator endpoint for DB & Mail status

### Template Types
- **EMAIL**: Text-based with optional subject
- **WHATSAPP**: Text messages or Image+Caption

## 📦 Project Structure

```
src/main/java/com/vibe/notification/
├── domain/                          # DDD: Core Business Logic
│   ├── model/                       # Records, Enums
│   │   ├── Channel.java             # EMAIL, WHATSAPP enum
│   │   ├── TemplateType.java        # TEXT, IMAGE enum
│   │   ├── NotificationStatus.java  # PENDING, SUCCESS, FAILED enum
│   │   └── NotificationRequest.java # Immutable request record
│   ├── service/                     # Domain Services
│   │   ├── TraceService.java        # MDC trace_id management
│   │   ├── TemplateResolutionService.java  # Language fallback logic
│   │   ├── TemplateRenderingService.java   # Thymeleaf rendering
│   │   └── NotificationDomainService.java  # Log management
│   └── exception/                   # Domain Exceptions
│
├── application/                     # Use Cases & Orchestration
│   ├── dto/                         # Request/Response DTOs
│   └── NotificationApplicationService.java  # Main orchestrator
│
├── infrastructure/                  # External Systems
│   ├── persistence/                 # JPA Entities & Repositories
│   │   ├── entity/
│   │   │   ├── NotificationTemplateEntity.java
│   │   │   └── NotificationLogEntity.java
│   │   └── repository/
│   ├── adapter/                     # Channel Adapters
│   │   ├── email/
│   │   │   ├── EmailNotificationAdapter.java
│   │   │   └── EmailProperties.java
│   │   └── whatsapp/
│   │       └── WhatsAppNotificationAdapter.java
│   └── external/                    # External API Clients
│       └── watzap/
│           ├── WatzapClient.java
│           ├── WatzapResponse.java
│           └── WatzapProperties.java
│
├── presentation/                    # REST API
│   ├── controller/
│   │   ├── NotificationController.java
│   │   └── NotificationHealthIndicator.java
│   └── error/
│       └── GlobalExceptionHandler.java
│
└── config/                          # Spring Configuration
    ├── AsyncConfig.java
    └── ThymeleafConfig.java

src/main/resources/
├── application.yml                  # Default configuration
├── application-test.yml             # Test configuration
└── db/migration/
    ├── V1__Create_notification_templates.sql
    └── V2__Create_notification_logs.sql

src/test/java/com/vibe/notification/
├── domain/service/
│   ├── TemplateResolutionServiceTest.java    # Unit test
│   └── TemplateRenderingServiceTest.java     # Unit test
└── integration/
    └── NotificationIntegrationTest.java      # E2E with Testcontainers
```

## 📋 Database Schema

### notification_templates
```sql
slug (PK)         VARCHAR(50)
language (PK)     VARCHAR(5)
channel           VARCHAR(20)  -- EMAIL, WHATSAPP
template_type     VARCHAR(20)  -- TEXT, IMAGE
subject           VARCHAR(255) -- Optional for Email
content           TEXT         -- Thymeleaf placeholders: [[${var}]]
image_url         TEXT         -- Optional for WhatsApp images
created_at        TIMESTAMP
updated_at        TIMESTAMP
```

### notification_logs
```sql
id (PK)           UUID
trace_id          UUID         -- Tracking identifier
recipient         VARCHAR(100)
slug              VARCHAR(50)
channel           VARCHAR(20)
variables         JSONB        -- Variable substitution data
status            VARCHAR(20)  -- PENDING, SUCCESS, FAILED
error_message     TEXT         -- Failure details
sent_at           TIMESTAMP
created_at        TIMESTAMP
```

Indexes on: `trace_id`, `recipient`, `status`, `created_at`

## 🔄 Request Flow

```
1. REST API receives POST /api/v1/notifications/send
2. TraceService generates trace_id and sets it in MDC
3. NotificationDomainService creates PENDING log entry
4. Response returned immediately (202 Accepted)
5. ProcessNotificationAsync() runs in background:
   a. TemplateResolutionService fetches template with fallback
   b. TemplateRenderingService renders content with variables
   c. Adapter sends via Email or WhatsApp
   d. Log updated to SUCCESS or FAILED
6. Logs are indexed by trace_id for correlation
```

## 📚 Documentation

### Getting Started
- **[QUICKSTART.md](QUICKSTART.md)** - 5-minute setup guide
- **[API_GUIDE.md](API_GUIDE.md)** - Complete API documentation with examples
- **[API_QUICK_REFERENCE.md](API_QUICK_REFERENCE.md)** - Quick reference with common commands

### Complete Documentation
- **[PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)** - Detailed project layout
- **[IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - Implementation details
- **[Swagger UI](http://localhost:8080/swagger-ui.html)** - Interactive API documentation

---

## 🚀 Quick Start

### Send Notification via HTTP API
```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "recipient": "+6281234567890",
    "slug": "welcome",
    "language": "en",
    "channel": "whatsapp",
    "variables": {"userName": "John Doe"}
  }'
```

### Send Notification via RabbitMQ
```python
import pika, json, uuid

conn = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
ch = conn.channel()
msg = {
    "traceId": str(uuid.uuid4()),
    "recipient": "+6281234567890",
    "slug": "welcome",
    "language": "en",
    "channel": "whatsapp",
    "variables": {"userName": "John Doe"}
}
ch.basic_publish(exchange='', routing_key='notification.request.queue',
    body=json.dumps(msg), properties=pika.BasicProperties(content_type='application/json'))
conn.close()
```

See [API_GUIDE.md](API_GUIDE.md) for complete examples in Python, Node.js, Java, Go, and more.

---



### Prerequisites
```bash
# Java 25+
# PostgreSQL 16+
# Maven 3.9+
```

### Local Setup
```bash
# Create database
createdb -U postgres notif_db

# Set environment variables
export DB_USERNAME=notif_user
export DB_PASSWORD=notif_pass
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
export WATZAP_API_KEY=your-watzap-key
export WATZAP_NUMBER_KEY=your-watzap-number

# Run application
mvn spring-boot:run
```

### Docker Compose (for PostgreSQL)
```bash
cd docker
docker-compose up -d
```

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Integration Tests Only
```bash
mvn test -Dtest=NotificationIntegrationTest
```

### Run Unit Tests
```bash
mvn test -Dtest=TemplateResolutionServiceTest,TemplateRenderingServiceTest
```

**Note**: Integration tests spin up PostgreSQL container via Testcontainers automatically.

## 📡 API Endpoints

### Send Notification
```http
POST /api/v1/notifications/send
Content-Type: application/json

{
  "recipient": "user@example.com",
  "slug": "welcome",
  "language": "id",
  "channel": "EMAIL",
  "variables": {
    "name": "John Doe",
    "companyName": "VibeCoding"
  }
}

Response (202 Accepted):
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "PENDING",
  "message": "Notification queued for processing"
}
```

### Health Check
```http
GET /api/v1/notifications/health

Response (200 OK):
Notification Service is healthy
```

### Spring Boot Actuator
```http
GET /actuator/health

Response:
{
  "status": "UP",
  "components": {
    "notificationHealthIndicator": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL connection OK",
        "mail": "SMTP connection OK"
      }
    }
  }
}
```

## 🛡️ Resilience Features

### Timeout Handling
- Connect Timeout: 5 seconds
- Read Timeout: 10 seconds
- Applied to: Email (JavaMail), Watzap.id API

### Retry Logic
- Watzap.id client has 2x retry with exponential backoff (500ms)

### Language Fallback
- Requested language → Default 'en'
- Prevents template not found errors

### Error Tracking
- All failures logged with error_message
- trace_id enables request correlation across logs

## 🔐 Configuration

### Environment Variables Required
```bash
DB_USERNAME          # PostgreSQL user
DB_PASSWORD          # PostgreSQL password
MAIL_USERNAME        # SMTP username (Gmail, etc.)
MAIL_PASSWORD        # SMTP password / app password
WATZAP_API_KEY       # Watzap.id API key
WATZAP_NUMBER_KEY    # Watzap.id number key
```

### Optional Overrides
```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/notif_db

# Mail
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587

# Watzap
WATZAP_BASE_URL=https://api.watzap.id/v1
WATZAP_TIMEOUT_CONNECT_MS=5000
WATZAP_TIMEOUT_READ_MS=10000
```

## 📊 Logging

All logs include `[traceId=xxx]` for distributed tracing:
```
2024-01-26 10:30:45 [main] INFO com.vibe.notification.application.NotificationApplicationService - [traceId=f47ac10b-58cc-4372-a567-0e02b2c3d479] - Processing notification request: recipient=user@example.com, slug=welcome
```

## 🔧 Development

### Add New Channel
1. Create adapter in `infrastructure/adapter/{channel}/`
2. Extend `NotificationApplicationService.processNotificationAsync()`
3. Add enum to `Channel.java`
4. Create integration test

### Add New Template Type
1. Update `TemplateType.java` enum
2. Modify adapters to handle new type
3. Add migration for `template_type` column

### Database Migration
```bash
# Flyway automatically runs migrations in db/migration/
# Naming: V{number}__description.sql

# Manual run:
mvn flyway:migrate
```

## 📝 Example Templates

### Email Welcome
```sql
INSERT INTO notification_templates (slug, language, channel, template_type, subject, content)
VALUES ('welcome', 'en', 'EMAIL', 'TEXT',
  'Welcome to [[${companyName}]]',
  '<h1>Hello [[${name}]]!</h1><p>Welcome to [[${companyName}]]!</p>'
);
```

### WhatsApp OTP
```sql
INSERT INTO notification_templates (slug, language, channel, template_type, content)
VALUES ('otp', 'en', 'WHATSAPP', 'TEXT',
  'Your verification code is: [[${code}]]. Valid for 10 minutes.'
);
```

### WhatsApp Image
```sql
INSERT INTO notification_templates (slug, language, channel, template_type, image_url, content)
VALUES ('promotion', 'en', 'WHATSAPP', 'IMAGE',
  'https://example.com/promo.jpg',
  'Check out our latest promotion! Valid until [[${expiryDate}]]'
);
```

## 🚨 Troubleshooting

### Template Not Found Error
- Check if template exists in DB for requested slug + language
- Verify fallback to English ('en') language is available
- Use query: `SELECT * FROM notification_templates WHERE slug = 'xyz';`

### Mail Send Failed
- Verify SMTP credentials (Gmail requires app password, not account password)
- Check network connectivity to SMTP server
- Review mail configuration in logs

### Watzap API Errors
- Verify API key and number key are correct
- Check phone number format (include country code)
- Review Watzap.id API documentation for error codes

### Async Processing Not Triggered
- Ensure `@EnableAsync` is active (check `AsyncConfig.java`)
- Verify thread pool is configured: `spring.task.execution.pool.*`
- Check application logs for async errors

## 📚 References

- [Spring Boot 3.4 Documentation](https://spring.io/projects/spring-boot)
- [Thymeleaf Documentation](https://www.thymeleaf.org/)
- [Testcontainers](https://www.testcontainers.org/)
- [Watzap.id API](https://watzap.id/docs)
- [Flyway Migrations](https://flywaydb.org/)

## 📄 License

This project is part of VibeCoding infrastructure.

---

**Last Updated**: January 26, 2026
**Status**: Production-Ready ✅
