# Notification Service

A high-performance, asynchronous notification service built with **Spring Boot 3.5+** and **Java 21**. Centralized multi-channel notifications via **WhatsApp (Watzap.id)** and **Email**.

## ✨ Key Features

- **🚀 Async Processing** - Non-blocking notification delivery with `@Async`
- **📧 Multi-Channel** - Email (Gmail SMTP) + WhatsApp (Watzap.id) support
- **🎯 Template Engine** - Thymeleaf-based rendering with variable substitution
- **🌍 Language Fallback** - Auto fallback from requested language → English
- **🔍 Distributed Tracing** - MDC-based trace_id for request tracking
- **📋 Audit Trail** - Complete notification log with PENDING/SUCCESS/FAILED status
- **⚡ Production Ready** - Health checks, metrics, full error handling

## 🚀 Quick Start

### Prerequisites
- **Java 21+**
- **PostgreSQL 16+** (or use Docker)
- **Maven 3.9+**
- **RabbitMQ** (for async messaging)

### 1. Clone & Setup
```bash
# Clone repository
git clone https://github.com/rofiksupriant/notification-service.git
cd notification-service

# Create .env file
cp .env.example .env

# Edit with your credentials
nano .env
```

### 2. Configure Database
```bash
# Create PostgreSQL database
createdb -U postgres notif_db

# Or use Docker Compose
cd docker
docker-compose up -d
```

### 3. Run Application
```bash
mvn spring-boot:run
```

The app will start at **http://localhost:8080**

## � Docker Deployment

### Local Development with Docker
```bash
# Build and run with Docker Compose
cd docker
docker-compose up --build

# Access the app
curl http://localhost:8080/actuator/health
```

### Production Deployment to Docker Hub

1. **Build and push to Docker Hub:**
```bash
# Windows
./build-and-push.bat your_docker_username 1.0.0

# Linux/macOS
./build-and-push.sh your_docker_username 1.0.0
```

2. **Deploy on server:**
```bash
# Copy docker-compose.prod.yml and .env.prod.example to server
scp docker/docker-compose.prod.yml user@server:/opt/notification-service/
scp .env.prod.example user@server:/opt/notification-service/.env.prod

# On server, edit .env.prod with your secrets
cd /opt/notification-service
nano .env.prod

# Start services
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

**Key Features:**
- ✅ PostgreSQL only accessible from app container (no exposed port)
- ✅ Health checks with automatic restart
- ✅ Non-root user in container
- ✅ Multi-stage build for minimal image size

**See [DOCKER_DEPLOYMENT.md](DOCKER_DEPLOYMENT.md) for complete guide.**

## �📋 Configuration

All settings use environment variables with `NOTIF_` prefix.

### Required Variables (Must Set)
```bash
NOTIF_MAIL_USERNAME=your-email@gmail.com
NOTIF_MAIL_PASSWORD=your-app-password
NOTIF_API_SECRET=your-secret-key
NOTIF_WATZAP_API_KEY=your-watzap-key
NOTIF_WATZAP_NUMBER_KEY=your-watzap-number
```

### Optional Variables (Have Defaults)
- Database: `NOTIF_DB_USERNAME`, `NOTIF_DB_PASSWORD`
- RabbitMQ: `NOTIF_RABBITMQ_HOST`, `NOTIF_RABBITMQ_PORT`
- Logging: `NOTIF_LOGGING_LEVEL_APP`
- And 40+ more...

**See [ENV_VARIABLES.md](ENV_VARIABLES.md) for complete reference.**

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│      REST API (Controllers)             │
├─────────────────────────────────────────┤
│   Application Layer (Use Cases)         │
├─────────────────────────────────────────┤
│ Domain Layer (Business Logic & Rules)   │
├─────────────────────────────────────────┤
│  Infrastructure (DB, Email, WhatsApp)   │
└─────────────────────────────────────────┘
```

### Layered Architecture
- **Domain** - Core business logic, entities, exceptions
- **Application** - Use cases, orchestration, async processing
- **Infrastructure** - Database, email, WhatsApp adapters
- **Presentation** - REST API, error handling

## 📚 Project Structure

```
src/
├── main/java/com/vibe/notification/
│   ├── domain/              # Business logic
│   │   ├── model/           # Entities, enums
│   │   ├── service/         # Domain services
│   │   └── exception/       # Exceptions
│   ├── application/         # Use cases
│   ├── infrastructure/      # Database, adapters
│   │   ├── persistence/     # JPA entities/repos
│   │   └── adapter/         # Channel adapters
│   ├── presentation/        # REST API
│   └── config/              # Spring config
│
└── main/resources/
    ├── application.yml      # Main config (env vars)
    ├── application-test.yml # Test config
    └── db/migration/        # Flyway migrations
```

## 🔌 API Endpoints

### Send Notification
```bash
POST /api/v1/notifications/send

{
  "slug": "welcome-template",
  "recipient": "user@example.com",
  "channel": "EMAIL",
  "language": "en",
  "variables": {
    "userName": "John Doe",
    "activationLink": "https://example.com/activate"
  }
}
```

**Response:** `202 Accepted` (processed asynchronously)

### Health Check
```bash
GET /actuator/health
```

### Swagger Documentation
```
http://localhost:8080/swagger-ui.html
```

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=RabbitMqIntegrationTest
```

### Test Coverage
- **61 tests** across unit, integration, and architecture tests
- **Testcontainers** for real PostgreSQL in tests
- **Mockito** for mocking external services

## 🔒 Security

**⚠️ Important:** Never commit `.env` file or hardcoded credentials!

- All secrets use environment variables (`NOTIF_*`)
- `.env` is in `.gitignore` for protection
- Use [.env.example](.env.example) as template
- See [SECURITY.md](SECURITY.md) for detailed guidelines

### Email Setup (Gmail)
1. Enable 2-Factor Authentication
2. Generate [App Password](https://myaccount.google.com/apppasswords)
3. Set `NOTIF_MAIL_PASSWORD` to the app password

### API Key
Generate secure key:
```bash
openssl rand -base64 32
```

## 📊 Database Schema

### notification_templates
```sql
slug (PK)         VARCHAR(50)
language (PK)     VARCHAR(5)
channel           VARCHAR(20)      -- EMAIL, WHATSAPP
template_type     VARCHAR(20)      -- TEXT, IMAGE
subject           VARCHAR(255)     -- Email only
content           TEXT             -- Thymeleaf: [[${var}]]
image_url         TEXT             -- WhatsApp images
created_at        TIMESTAMP
updated_at        TIMESTAMP
```

### notification_logs
```sql
id (PK)           UUID
trace_id          UUID             -- Tracking
recipient         VARCHAR(100)
slug              VARCHAR(50)
channel           VARCHAR(20)
variables         JSONB            -- Variable data
status            VARCHAR(20)      -- PENDING/SUCCESS/FAILED
error_message     TEXT
sent_at           TIMESTAMP
created_at        TIMESTAMP
```

## 🎨 Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 (Records, Pattern Matching) |
| Framework | Spring Boot 3.5+ |
| Database | PostgreSQL 16 |
| ORM | JPA / Hibernate |
| Async | RabbitMQ / @Async |
| Template | Thymeleaf |
| Testing | JUnit 5, Mockito, Testcontainers |
| Build | Maven 3.9+ |
| Docs | SpringDoc OpenAPI (Swagger) |

## � Documentation

- **[docs/README.md](docs/README.md)** - Documentation index & navigation
- **[docs/deployment/QUICK_DEPLOY.md](docs/deployment/QUICK_DEPLOY.md)** - 5-minute deploy guide
- **[docs/deployment/SETUP.md](docs/deployment/SETUP.md)** - Local development setup
- **[docs/deployment/WORKFLOW.md](docs/deployment/WORKFLOW.md)** - Full deployment workflow
- **[docs/deployment/CHECKLIST.md](docs/deployment/CHECKLIST.md)** - Verification checklist
- **[docs/docker/OVERVIEW.md](docs/docker/OVERVIEW.md)** - Docker concepts & setup
- **[docs/reference/COMMANDS.md](docs/reference/COMMANDS.md)** - Docker commands reference
- **[docs/reference/ENVIRONMENT.md](docs/reference/ENVIRONMENT.md)** - Environment variables
- **[docs/reference/TROUBLESHOOTING.md](docs/reference/TROUBLESHOOTING.md)** - Common issues & fixes
- **[ENV_VARIABLES.md](ENV_VARIABLES.md)** - Detailed configuration reference
- **[SECURITY.md](SECURITY.md)** - Security guidelines

## 🐛 Troubleshooting

### Application won't start
```bash
# Check if required env vars are set
echo $NOTIF_MAIL_USERNAME
echo $NOTIF_API_SECRET

# Verify database connection
psql -U postgres -d notif_db -c "SELECT 1"
```

### Template rendering fails
- Check template syntax uses `[[${variableName}]]` (Thymeleaf)
- Verify variables are passed in request
- Check logs for template resolution errors

### RabbitMQ connection fails
```bash
# Verify RabbitMQ is running
docker ps | grep rabbitmq

# Or disable RabbitMQ
export NOTIF_RABBITMQ_ENABLED=false
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes and write tests
4. Ensure all tests pass: `mvn clean test`
5. Submit a pull request

## 📄 License

This project is provided as-is for educational and commercial use.

## 📞 Support

For issues, feature requests, or questions:
- Check [ENV_VARIABLES.md](ENV_VARIABLES.md) for configuration help
- Review [SECURITY.md](SECURITY.md) for security concerns
- Check application logs: `tail -f logs/notification-service.log`

---

**Happy notifying! 🎉**
