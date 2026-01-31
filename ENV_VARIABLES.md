# Environment Variables Reference

Complete list of all configurable environment variables for the Notification Service.

All variables use the `NOTIF_` prefix to avoid conflicts with other applications.

## Application Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_APP_NAME` | `notification-service` | Spring application name |

## JPA & Hibernate Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_JPA_DDL_AUTO` | `validate` | Hibernate DDL auto strategy (validate/create/create-drop/update) |
| `NOTIF_HIBERNATE_DIALECT` | `org.hibernate.dialect.PostgreSQLDialect` | Hibernate SQL dialect |
| `NOTIF_HIBERNATE_BATCH_SIZE` | `10` | Hibernate JDBC batch size |
| `NOTIF_HIBERNATE_ORDER_INSERTS` | `true` | Order INSERT statements |
| `NOTIF_HIBERNATE_ORDER_UPDATES` | `true` | Order UPDATE statements |

## Database Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_DB_URL` | `jdbc:postgresql://localhost:5432/notif_db` | Database JDBC URL |
| `NOTIF_DB_USERNAME` | `postgres` | Database username |
| `NOTIF_DB_PASSWORD` | `postgres` | Database password |
| `NOTIF_DB_HIKARI_MAX_POOL_SIZE` | `10` | Maximum Hikari connection pool size |
| `NOTIF_DB_HIKARI_MIN_IDLE` | `5` | Minimum idle connections in Hikari pool |

## Flyway Database Migrations

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_FLYWAY_ENABLED` | `true` | Enable Flyway migrations |
| `NOTIF_FLYWAY_LOCATIONS` | `classpath:db/migration` | Flyway migration location |
| `NOTIF_FLYWAY_BASELINE_ON_MIGRATE` | `true` | Baseline existing database on migrate |

## Email Configuration (Gmail SMTP)

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_MAIL_HOST` | `smtp.gmail.com` | SMTP server hostname |
| `NOTIF_MAIL_PORT` | `587` | SMTP server port (TLS) |
| `NOTIF_MAIL_USERNAME` | *(required)* | Gmail email address |
| `NOTIF_MAIL_PASSWORD` | *(required)* | Gmail app password |
| `NOTIF_MAIL_SMTP_AUTH` | `true` | Enable SMTP authentication |
| `NOTIF_MAIL_SMTP_STARTTLS_ENABLE` | `true` | Enable STARTTLS |
| `NOTIF_MAIL_SMTP_STARTTLS_REQUIRED` | `true` | Require STARTTLS |
| `NOTIF_MAIL_SMTP_CONNECTION_TIMEOUT` | `5000` | SMTP connection timeout (ms) |
| `NOTIF_MAIL_SMTP_TIMEOUT` | `10000` | SMTP read timeout (ms) |
| `NOTIF_MAIL_SMTP_WRITE_TIMEOUT` | `10000` | SMTP write timeout (ms) |

**Note:** Use [Gmail App Passwords](https://myaccount.google.com/apppasswords) instead of your regular password.

## Task Execution Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_TASK_EXECUTION_CORE_SIZE` | `5` | Core thread pool size for async execution |
| `NOTIF_TASK_EXECUTION_MAX_SIZE` | `10` | Max thread pool size for async execution |
| `NOTIF_TASK_EXECUTION_QUEUE_CAPACITY` | `100` | Task queue capacity |
| `NOTIF_TASK_SCHEDULING_POOL_SIZE` | `2` | Scheduled task pool size |

## RabbitMQ Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_RABBITMQ_HOST` | `localhost` | RabbitMQ broker hostname |
| `NOTIF_RABBITMQ_PORT` | `5672` | RabbitMQ broker port |
| `NOTIF_RABBITMQ_USERNAME` | `guest` | RabbitMQ username |
| `NOTIF_RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `NOTIF_RABBITMQ_VIRTUAL_HOST` | `/` | RabbitMQ virtual host |
| `NOTIF_RABBITMQ_CONNECTION_TIMEOUT` | `10000` | RabbitMQ connection timeout (ms) |
| `NOTIF_RABBITMQ_CONCURRENCY` | `3` | Min consumer concurrency |
| `NOTIF_RABBITMQ_MAX_CONCURRENCY` | `10` | Max consumer concurrency |
| `NOTIF_RABBITMQ_PREFETCH` | `1` | Prefetch count per consumer |
| `NOTIF_RABBITMQ_AUTO_STARTUP` | `true` | Auto-start RabbitMQ listener |
| `NOTIF_RABBITMQ_ACKNOWLEDGE_MODE` | `AUTO` | Message acknowledgement mode |
| `NOTIF_RABBITMQ_ENABLED` | `true` | Enable RabbitMQ feature |

## API Security

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_API_SECRET` | *(required)* | API secret key (min 32 chars) |

## WhatsApp Integration (Watzap.id)

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_WATZAP_API_KEY` | *(required)* | Watzap.id API key |
| `NOTIF_WATZAP_NUMBER_KEY` | *(required)* | Watzap.id number key |
| `NOTIF_WATZAP_BASE_URL` | `https://api.watzap.id/v1` | Watzap.id API base URL |
| `NOTIF_WATZAP_TIMEOUT_CONNECT_MS` | `5000` | Watzap API connection timeout (ms) |
| `NOTIF_WATZAP_TIMEOUT_READ_MS` | `10000` | Watzap API read timeout (ms) |

Get your credentials from [Watzap.id Dashboard](https://watzap.id/dashboard).

## Logging Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_LOGGING_LEVEL_ROOT` | `INFO` | Root logger level |
| `NOTIF_LOGGING_LEVEL_APP` | `DEBUG` | App logger level (com.vibe.notification) |
| `NOTIF_LOGGING_PATTERN_CONSOLE` | `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - [traceId=%X{traceId}] - %msg%n` | Console log format |
| `NOTIF_LOGGING_PATTERN_FILE` | `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - [traceId=%X{traceId}] - %msg%n` | File log format |
| `NOTIF_LOGGING_FILE_NAME` | `logs/notification-service.log` | Log file path |

## Management & Actuator Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_MANAGEMENT_ENDPOINTS` | `health,info,metrics,loggers` | Actuator endpoints to expose |
| `NOTIF_MANAGEMENT_HEALTH_DETAILS` | `always` | Health check detail level |

## Swagger/OpenAPI Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIF_SWAGGER_ENABLED` | `true` | Enable Swagger UI |
| `NOTIF_SWAGGER_PATH` | `/swagger-ui.html` | Swagger UI path |
| `NOTIF_SWAGGER_OPERATIONS_SORTER` | `method` | Operations sort order |
| `NOTIF_SWAGGER_TAG_SORTER` | `alpha` | Tag sort order |
| `NOTIF_SWAGGER_DISPLAY_OPERATION_ID` | `false` | Display operation ID |
| `NOTIF_SWAGGER_DEEP_LINKING` | `true` | Enable deep linking |
| `NOTIF_SWAGGER_API_DOCS_PATH` | `/v3/api-docs` | OpenAPI docs path |
| `NOTIF_SWAGGER_SHOW_ACTUATOR` | `false` | Show actuator endpoints in Swagger |
| `NOTIF_SWAGGER_USE_FQN_FOR_PARAMETER_NAME` | `true` | Use FQN for parameter names |

---

## Setup Instructions

1. **Copy `.env.example` to `.env`:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual values:**
   ```bash
   nano .env
   ```

3. **Update required credentials:**
   - `NOTIF_DB_PASSWORD`
   - `NOTIF_MAIL_USERNAME`
   - `NOTIF_MAIL_PASSWORD`
   - `NOTIF_API_SECRET`
   - `NOTIF_WATZAP_API_KEY`
   - `NOTIF_WATZAP_NUMBER_KEY`

4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

The application will automatically load all variables from `.env` and use them as overrides.

---

## Production Deployment

For production, **never use `.env` files**. Instead:

1. **Use environment variables directly:**
   ```bash
   export NOTIF_DB_PASSWORD=prod-password
   export NOTIF_API_SECRET=prod-secret
   # ... etc
   ```

2. **Or use a secrets management service:**
   - AWS Secrets Manager
   - Azure Key Vault
   - HashiCorp Vault
   - Kubernetes Secrets

3. **Docker environment:**
   ```bash
   docker run -e NOTIF_DB_PASSWORD=xyz -e NOTIF_API_SECRET=abc ...
   ```

---

## Secure Key Generation

Generate secure values:

```bash
# Linux/Mac - API Secret
openssl rand -base64 32

# PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))

# Python
python -c "import secrets; print(secrets.token_urlsafe(32))"
```
