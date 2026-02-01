# Environment Variables Reference

Complete list of all configuration variables for the Notification Service.

All variables use the `NOTIF_` prefix to avoid conflicts.

---

## Docker Configuration

Used when deploying with Docker/Docker Compose.

| Variable | Required | Example | Description |
|----------|----------|---------|-------------|
| `DOCKER_USERNAME` | ✅ | `rofiksupriant` | Docker Hub username for image |
| `APP_VERSION` | ✅ | `1.0.0` | Application version/image tag |
| `DB_HOST` | ✅ | `notification-db` | Database hostname |
| `DB_PORT` | ✅ | `5432` | Database port |
| `DB_NAME` | ✅ | `notif_db` | Database name |
| `DB_USER` | ✅ | `postgres` | Database username |
| `DB_PASSWORD` | ✅ | `SecurePass123!` | Database password (16+ chars recommended) |

---

## Application Configuration

Core application settings.

| Variable | Default | Example | Description |
|----------|---------|---------|-------------|
| `NOTIF_APP_NAME` | `notification-service` | `notification-service` | Spring application name |
| `NOTIF_API_SECRET` | - | `abc123xyz789` | API authentication secret (32+ chars) |
| `NOTIF_SERVER_PORT` | `8080` | `8080` | Server port (don't change) |

---

## Database Configuration

PostgreSQL connection settings.

| Variable | Default | Example | Description |
|----------|---------|---------|-------------|
| `NOTIF_DB_URL` | `jdbc:postgresql://localhost:5432/notif_db` | See default | JDBC connection URL |
| `NOTIF_DB_USERNAME` | `postgres` | `postgres` | Database username |
| `NOTIF_DB_PASSWORD` | `postgres` | `YourPassword123!` | Database password |
| `NOTIF_DB_HIKARI_MAX_POOL_SIZE` | `10` | `10` | Max connection pool size |
| `NOTIF_DB_HIKARI_MIN_IDLE` | `5` | `5` | Min idle connections |
| `NOTIF_FLYWAY_ENABLED` | `true` | `true` | Enable Flyway migrations |
| `NOTIF_FLYWAY_LOCATIONS` | `classpath:db/migration` | - | Migration files location |
| `NOTIF_JPA_DDL_AUTO` | `validate` | `validate` | Hibernate DDL strategy |

---

## Email Configuration (Gmail)

For sending email notifications.

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `NOTIF_MAIL_ENABLED` | `true` | ✅ | Enable email feature |
| `NOTIF_MAIL_HOST` | `smtp.gmail.com` | ✅ | SMTP server address |
| `NOTIF_MAIL_PORT` | `587` | ✅ | SMTP port (587 = TLS, 465 = SSL) |
| `NOTIF_MAIL_USERNAME` | - | ✅ | Gmail address |
| `NOTIF_MAIL_PASSWORD` | - | ✅ | Gmail app password (not regular password!) |
| `NOTIF_MAIL_FROM` | `noreply@company.com` | ✅ | From address in emails |

**Gmail Setup:**
1. Enable 2-Factor Authentication
2. Go to: https://myaccount.google.com/apppasswords
3. Generate app password
4. Use that password in `NOTIF_MAIL_PASSWORD` (NOT your regular password)

---

## WhatsApp Configuration (Watzap.id)

For sending WhatsApp notifications.

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `NOTIF_WATZAP_ENABLED` | `true` | ✅ | Enable WhatsApp feature |
| `NOTIF_WATZAP_API_KEY` | - | ✅ | Watzap API key |
| `NOTIF_WATZAP_PHONE_ID` | - | ✅ | WhatsApp phone ID |

**Watzap Setup:**
1. Create account: https://watzap.id
2. Go to Dashboard
3. Copy API Key and Phone ID
4. Use in environment variables

---

## RabbitMQ Configuration

For async message processing (optional).

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `NOTIF_RABBITMQ_ENABLED` | `false` | ❌ | Enable RabbitMQ async (optional) |
| `NOTIF_RABBITMQ_HOST` | `rabbitmq` | ❌ | RabbitMQ server address |
| `NOTIF_RABBITMQ_PORT` | `5672` | ❌ | RabbitMQ port |
| `NOTIF_RABBITMQ_USERNAME` | `guest` | ❌ | RabbitMQ username |
| `NOTIF_RABBITMQ_PASSWORD` | `guest` | ❌ | RabbitMQ password |
| `NOTIF_RABBITMQ_VIRTUAL_HOST` | `/` | ❌ | RabbitMQ virtual host |

**Note:** RabbitMQ is optional. Disabled by default. Leave as-is unless you specifically need async messaging.

---

## Logging Configuration

Log output settings.

| Variable | Default | Example | Description |
|----------|---------|---------|-------------|
| `NOTIF_LOGGING_LEVEL_ROOT` | `INFO` | `INFO` | Root logger level |
| `NOTIF_LOGGING_LEVEL_APP` | `INFO` | `INFO` | App logger level |
| `NOTIF_LOGGING_FORMAT` | `JSON` | `JSON` | Log format (JSON or plain) |
| `NOTIF_LOGGING_INCLUDE_TRACE` | `false` | `false` | Include stack traces in logs |
| `SPRING_JPA_SHOW_SQL` | `false` | `false` | Log SQL queries |

**Levels:** TRACE, DEBUG, INFO, WARN, ERROR

---

## Server Configuration

Tomcat & servlet settings.

| Variable | Default | Example | Description |
|----------|---------|---------|-------------|
| `SERVER_PORT` | `8080` | `8080` | Server port |
| `SERVER_SERVLET_CONTEXT_PATH` | `/` | `/` | Context root path |
| `SERVER_COMPRESSION_ENABLED` | `true` | `true` | Enable gzip compression |
| `SERVER_TOMCAT_THREADS_MAX` | `200` | `200` | Max Tomcat threads |

---

## Swagger/OpenAPI Configuration

API documentation settings.

| Variable | Default | Example | Description |
|----------|---------|---------|-------------|
| `NOTIF_SWAGGER_ENABLED` | `true` | `true` | Enable Swagger UI |
| `NOTIF_SWAGGER_TITLE` | `Notification Service API` | - | API title |
| `NOTIF_SWAGGER_DESCRIPTION` | `Multi-channel notifications` | - | API description |
| `NOTIF_SWAGGER_VERSION` | `1.0.0` | - | API version |

**Access:** http://localhost:8080/swagger-ui.html

---

## Management/Actuator Configuration

Health checks and monitoring.

| Variable | Default | Example | Description |
|----------|---------|---------|-------------|
| `NOTIF_MANAGEMENT_ENDPOINTS` | `health,info,metrics,loggers` | - | Exposed endpoints |
| `NOTIF_MANAGEMENT_HEALTH_DETAILS` | `always` | `always` | Health detail level |
| `NOTIF_MANAGEMENT_HEALTH_DB_ENABLED` | `true` | `true` | Check database health |
| `NOTIF_MANAGEMENT_METRICS_ENABLED` | `true` | `true` | Enable metrics |

**Endpoints:**
- `/actuator/health` - Application health
- `/actuator/info` - Application info
- `/actuator/metrics` - Performance metrics

---

## Complete Example (.env File)

```bash
# ========== DOCKER ==========
DOCKER_USERNAME=rofiksupriant
APP_VERSION=1.0.0

# ========== DATABASE ==========
DB_HOST=notification-db
DB_PORT=5432
DB_NAME=notif_db
DB_USER=postgres
DB_PASSWORD=SuperSecurePassword123!

# ========== APPLICATION ==========
NOTIF_API_SECRET=GeneratedRandomSecret123456789

# ========== EMAIL ==========
NOTIF_MAIL_ENABLED=true
NOTIF_MAIL_HOST=smtp.gmail.com
NOTIF_MAIL_PORT=587
NOTIF_MAIL_USERNAME=your_email@gmail.com
NOTIF_MAIL_PASSWORD=xxxx xxxx xxxx xxxx
NOTIF_MAIL_FROM=noreply@yourcompany.com

# ========== WHATSAPP ==========
NOTIF_WATZAP_ENABLED=true
NOTIF_WATZAP_API_KEY=your_watzap_api_key
NOTIF_WATZAP_PHONE_ID=your_phone_id

# ========== LOGGING ==========
NOTIF_LOGGING_LEVEL_ROOT=INFO
NOTIF_LOGGING_LEVEL_APP=INFO
SPRING_JPA_SHOW_SQL=false

# ========== SERVER ==========
SERVER_PORT=8080
SERVER_SERVLET_CONTEXT_PATH=/
```

---

## Setup Instructions

### 1. Copy Template

```bash
cp .env.example .env
```

### 2. Edit Configuration

```bash
nano .env
```

### 3. Update Required Values

Edit these **minimum required** values:

```bash
DOCKER_USERNAME=your_username              # Docker Hub username
APP_VERSION=1.0.0                         # Version
DB_PASSWORD=YourStrongPassword123!        # (16+ chars)
NOTIF_API_SECRET=RandomSecretKey          # (32+ chars)
NOTIF_MAIL_USERNAME=your_email@gmail.com  # Your Gmail
NOTIF_MAIL_PASSWORD=app_specific_pass     # Gmail app password
NOTIF_WATZAP_API_KEY=your_key            # Watzap key
NOTIF_WATZAP_PHONE_ID=your_phone_id      # Watzap phone
```

### 4. Secure .env File

```bash
chmod 600 .env
# Now only owner can read
```

### 5. Verify

```bash
# Check values are set
cat .env | grep -v "^#" | grep "^[A-Z]"

# Should show all variables
```

---

## Security Notes

### ✅ DO

- Store passwords in `.env` file only
- Use strong passwords (16+ characters, mix upper/lower/numbers/symbols)
- Generate API secrets: `openssl rand -base64 32`
- Keep `.env` file secure: `chmod 600 .env`
- Use Gmail app passwords (not regular password)
- Rotate credentials regularly
- Back up `.env` to secure location (NOT git)

### ❌ DON'T

- Commit `.env` to git
- Put real credentials in `.env.example`
- Share `.env` file via email/chat
- Use same password for multiple environments
- Hardcode credentials in code
- Log sensitive values
- Commit credentials to repository

---

## Generate Secure Values

### API Secret (32+ characters)

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))

# Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

### Database Password

```bash
# Generate 16 char password
openssl rand -base64 12

# Or manually: Mix uppercase, lowercase, numbers, symbols
# Example: Abc@123XyZ!pQrS
```

---

## Local Development vs Production

### Local (.env for local development)

```bash
NOTIF_DB_URL=jdbc:postgresql://localhost:5432/notif_db
NOTIF_DB_USERNAME=postgres
NOTIF_DB_PASSWORD=postgres          # Simple password OK for local
NOTIF_LOGGING_LEVEL_APP=DEBUG       # Debug logging
SPRING_JPA_SHOW_SQL=true           # Show SQL queries
```

### Production (.env for Docker Compose production)

```bash
NOTIF_DB_URL=jdbc:postgresql://notification-db:5432/notif_db
NOTIF_DB_USERNAME=postgres
NOTIF_DB_PASSWORD=SuperSecurePassword123!  # Strong password required!
NOTIF_LOGGING_LEVEL_APP=INFO       # Info only
SPRING_JPA_SHOW_SQL=false          # Don't log SQL (performance)
```

---

## Troubleshooting

### "Required variable not set" Error

```bash
# Check which variables are missing
grep "^NOTIF_" .env | sort

# Compare with .env.example
diff .env .env.example

# Add missing variables
nano .env
```

### "Connection refused" for Database

```bash
# Verify these are set correctly:
NOTIF_DB_URL=jdbc:postgresql://notification-db:5432/notif_db
NOTIF_DB_USERNAME=postgres
NOTIF_DB_PASSWORD=correct_password

# Note: Use 'notification-db' (service name) not 'localhost' in Docker
```

### "Authentication failed" for Email

```bash
# Verify Gmail app password (NOT regular password)
# 1. Go to https://myaccount.google.com/apppasswords
# 2. Generate new app password
# 3. Use that value for NOTIF_MAIL_PASSWORD

# Should be 16 chars with spaces: "xxxx xxxx xxxx xxxx"
```

---

## Reference Links

- 📖 [Spring Boot Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- 🐳 [Docker Compose Environment Variables](https://docs.docker.com/compose/environment-variables/)
- 📧 [Gmail App Passwords](https://support.google.com/accounts/answer/185833)
- 💬 [Watzap.id Documentation](https://watzap.id/docs)

---

**Last Updated:** January 31, 2026  
**Status:** ✅ Current and Complete
