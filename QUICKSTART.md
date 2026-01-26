# Quick Start Guide

## 📋 Prerequisites

- **Java 25+** (ensure `java -version` shows Java 25)
- **Maven 3.9+**
- **PostgreSQL 16+** (or Docker)
- **Git**

## 🚀 Setup (5 minutes)

### Step 1: Start PostgreSQL

**Option A: Docker**
```bash
cd notification-service/docker
docker-compose up -d
# Wait for healthcheck to pass (10 seconds)
```

**Option B: Local PostgreSQL**
```bash
# Create database
createdb -U postgres notif_db

# Create user
psql -U postgres -c "CREATE USER notif_user WITH PASSWORD 'notif_pass';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE notif_db TO notif_user;"
```

### Step 2: Set Environment Variables

```bash
# Linux/macOS
export DB_USERNAME=notif_user
export DB_PASSWORD=notif_pass
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
export WATZAP_API_KEY=your-watzap-key
export WATZAP_NUMBER_KEY=your-watzap-number

# Windows (PowerShell)
$env:DB_USERNAME = "notif_user"
$env:DB_PASSWORD = "notif_pass"
# ... etc
```

### Step 3: Build & Run

```bash
cd notification-service

# Build
mvn clean install

# Run
mvn spring-boot:run

# Or use IDE: Run NotificationServiceApplication.java
```

The application starts on `http://localhost:8080`

## ✅ Verify Installation

```bash
# Check health endpoint
curl http://localhost:8080/api/v1/notifications/health

# Expected response:
# Notification Service is healthy
```

## 📊 View Logs

```bash
# Logs appear in console:
2024-01-26 10:30:45 [main] INFO [...] - Tomcat started
2024-01-26 10:30:46 [main] INFO [...] - Started NotificationServiceApplication
```

## 🧪 Run Tests

```bash
# All tests (includes Testcontainers)
mvn test

# Only unit tests
mvn test -Dtest=TemplateResolutionServiceTest,TemplateRenderingServiceTest

# Only integration tests
mvn test -Dtest=NotificationIntegrationTest
```

## 📡 Send Your First Notification

### 1. Insert a Template

```sql
-- Connect to PostgreSQL
psql -U notif_user -d notif_db

-- Insert template
INSERT INTO notification_templates (slug, language, channel, template_type, subject, content)
VALUES ('welcome', 'en', 'EMAIL', 'TEXT',
  'Welcome to VibeCoding',
  'Hello [[${name}]], welcome to [[${company}]]!');
```

### 2. Send Notification via API

```bash
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipient": "user@example.com",
    "slug": "welcome",
    "language": "en",
    "channel": "EMAIL",
    "variables": {
      "name": "John Doe",
      "company": "VibeCoding"
    }
  }'
```

### 3. Check Response

```json
{
  "logId": "550e8400-e29b-41d4-a716-446655440000",
  "traceId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "PENDING",
  "message": "Notification queued for processing"
}
```

### 4. Monitor Processing

```sql
-- Check notification logs
SELECT id, trace_id, recipient, status, error_message, created_at 
FROM notification_logs 
ORDER BY created_at DESC 
LIMIT 10;
```

## 🏥 Health Checks

```bash
# Simple health
curl http://localhost:8080/api/v1/notifications/health

# Detailed health (with DB & Mail status)
curl http://localhost:8080/actuator/health
```

## 🐛 Debugging

### Enable Debug Logs

**Option 1: application.yml**
```yaml
logging:
  level:
    com.vibe.notification: DEBUG
    org.hibernate: DEBUG
```

**Option 2: Runtime (no restart)**
```bash
curl -X POST http://localhost:8080/actuator/loggers/com.vibe.notification \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

### View Trace Logs

```bash
# All logs include [traceId=xxx]
tail -f logs/notification-service.log | grep "f47ac10b-58cc"
```

### Check Database State

```sql
-- See pending notifications
SELECT * FROM notification_logs WHERE status = 'PENDING';

-- See failed notifications
SELECT id, recipient, error_message FROM notification_logs WHERE status = 'FAILED';

-- View templates
SELECT slug, language, channel, template_type FROM notification_templates;
```

## 🔑 API Endpoints Reference

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/notifications/send` | Send notification (202 Accepted) |
| GET | `/api/v1/notifications/health` | Simple health check |
| GET | `/actuator/health` | Detailed health (with DB/Mail) |
| GET | `/actuator/info` | Application info |
| GET | `/actuator/metrics` | Performance metrics |

## 📝 Example Templates to Insert

### Email Order Confirmation
```sql
INSERT INTO notification_templates (slug, language, channel, template_type, subject, content)
VALUES ('order_confirmation', 'en', 'EMAIL', 'TEXT',
  'Order #[[${orderId}]] Confirmed',
  'Thank you for your order. Order #[[${orderId}]] confirmed for [[${amount}]]');
```

### WhatsApp OTP
```sql
INSERT INTO notification_templates (slug, language, channel, template_type, content)
VALUES ('otp', 'en', 'WHATSAPP', 'TEXT',
  'Your verification code is: [[${code}]]. Valid for 10 minutes.');
```

### WhatsApp Image
```sql
INSERT INTO notification_templates (slug, language, channel, template_type, image_url, content)
VALUES ('promotion', 'en', 'WHATSAPP', 'IMAGE',
  'https://example.com/promo.jpg',
  'Limited time offer! Use code [[${coupon}]] for 20% off.');
```

## ⚠️ Common Issues

### Issue: "Template not found"
**Solution**: Insert template via SQL first, or use language fallback to 'en'

### Issue: "SMTP connection failed"
**Solution**: Check Gmail app password (not account password), ensure 2FA enabled

### Issue: "Watzap API error"
**Solution**: Verify API key, number key, and phone number format

### Issue: Tests fail with "Connection refused"
**Solution**: Testcontainers will auto-pull PostgreSQL image (~400MB), ensure Docker running

## 📚 Next Steps

1. **Read [README.md](README.md)** - Full documentation
2. **Read [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)** - What was implemented
3. **Review [DOCS_INSTRUCTION.md](DOCS_INSTRUCTION.md)** - Architecture guidelines
4. **Explore code** - Start from [NotificationController.java](src/main/java/com/vibe/notification/presentation/controller/NotificationController.java)

## 💡 Pro Tips

- Use `trace_id` to correlate requests across logs
- Set all environment variables before running (no hot reload)
- Run tests regularly to catch regressions
- Check `/actuator/health` as part of deployment health checks
- Monitor async processing via database status updates

## 🆘 Need Help?

1. Check logs: `logs/notification-service.log`
2. Review database: `notification_logs` and `notification_templates`
3. Test health: `GET /actuator/health`
4. See [README.md Troubleshooting](README.md#-troubleshooting) section

---

**Ready to go!** 🎉 Run `mvn spring-boot:run` and start sending notifications.
