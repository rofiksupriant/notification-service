# Notification Service - Complete Configuration Guide

## Table of Contents
1. [Environment Variables](#environment-variables)
2. [Application Configuration](#application-configuration)
3. [Database Setup](#database-setup)
4. [RabbitMQ Setup](#rabbitmq-setup)
5. [Email Configuration](#email-configuration)
6. [WhatsApp Configuration](#whatsapp-configuration)
7. [Deployment](#deployment)
8. [Troubleshooting](#troubleshooting)

---

## Environment Variables

Set these environment variables before running the application:

```bash
# Database Configuration
export DB_URL=jdbc:postgresql://localhost:5432/notification_db
export DB_USERNAME=notif_user
export DB_PASSWORD=secure_password

# Mail Configuration
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-specific-password
export MAIL_FROM=noreply@example.com

# WhatsApp (Watzap.id) Configuration
export WATZAP_API_KEY=your-watzap-api-key
export WATZAP_API_URL=https://api.watzap.id/v1
export WATZAP_NUMBER_KEY=your-watzap-number-key

# RabbitMQ Configuration
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=guest
export RABBITMQ_PASSWORD=guest

# API Key
export API_KEY_SECRET=your-api-key-secret

# Application
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod
```

---

## Application Configuration

### Default Profile (application.yml)

```yaml
spring:
  application:
    name: notification-service
    
  # DataSource Configuration
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/notification_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true
  
  # Mail Configuration
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
      mail.smtp.connectiontimeout: 5000
      mail.smtp.timeout: 10000
      mail.smtp.writetimeout: 5000
  
  # RabbitMQ Configuration
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: /
    connection-timeout: 10000
  
  # Async Configuration
  task:
    execution:
      pool:
        core-size: 4
        max-size: 8
        queue-capacity: 100
        keep-alive: 60s
        allow-core-thread-timeout: true
  
  # Thymeleaf Configuration
  thymeleaf:
    cache: true
    check-template-location: true

# Application Properties
app:
  feature:
    rabbitmq:
      enabled: ${RABBITMQ_ENABLED:true}
  
  api:
    key:
      secret: ${API_KEY_SECRET:changeme}

# Logging
logging:
  level:
    root: INFO
    com.vibe: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - [traceId=%X{traceId}] - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - [traceId=%X{traceId}] - %msg%n"
  file:
    name: logs/notification-service.log
    max-size: 10MB
    max-history: 10

# Server Configuration
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /
  compression:
    enabled: true
    min-response-size: 1024

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

# WhatsApp Configuration (Custom)
watzap:
  api-key: ${WATZAP_API_KEY:}
  api-url: ${WATZAP_API_URL:https://api.watzap.id/v1}
  number-key: ${WATZAP_NUMBER_KEY:}
  connect-timeout: 5000
  read-timeout: 10000
```

### Production Profile (application-prod.yml)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
  
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 500

logging:
  level:
    root: WARN
    com.vibe: INFO
  file:
    name: logs/notification-service.log
    max-size: 50MB
    max-history: 30

server:
  compression:
    enabled: true
    min-response-size: 512
```

### Test Profile (application-test.yml)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
  
  flyway:
    enabled: false

app:
  feature:
    rabbitmq:
      enabled: true

api:
  key:
    secret: test-api-key
```

---

## Database Setup

### PostgreSQL Installation

```bash
# macOS (Homebrew)
brew install postgresql
brew services start postgresql

# Ubuntu/Debian
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql

# Docker
docker run --name postgres-notif \
  -e POSTGRES_DB=notification_db \
  -e POSTGRES_USER=notif_user \
  -e POSTGRES_PASSWORD=secure_password \
  -p 5432:5432 \
  -d postgres:16-alpine
```

### Create Database

```sql
-- Connect as superuser
psql -U postgres

-- Create user
CREATE USER notif_user WITH PASSWORD 'secure_password';

-- Create database
CREATE DATABASE notification_db OWNER notif_user;

-- Grant privileges
GRANT CONNECT ON DATABASE notification_db TO notif_user;
GRANT USAGE ON SCHEMA public TO notif_user;
GRANT CREATE ON SCHEMA public TO notif_user;

-- Enable JSONB extension
\c notification_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

### Initial Data

```sql
-- Insert sample templates
INSERT INTO notification_templates (slug, language, channel, template_type, subject, content, created_at, updated_at)
VALUES 
  ('welcome', 'en', 'EMAIL', 'TEXT', 
   'Welcome to Our Service', 
   'Hello [[${userName}]], welcome to our service. Click here to activate: [[${activationLink}]]',
   NOW(), NOW()),
  ('otp', 'en', 'WHATSAPP', 'TEXT',
   NULL,
   'Your OTP is: [[${otp_code}]]. Valid for [[${expiry_minutes}]] minutes.',
   NOW(), NOW()),
  ('welcome', 'id', 'WHATSAPP', 'TEXT',
   NULL,
   'Halo [[${userName}]], selamat datang! Klik tautan ini untuk aktivasi: [[${activationLink}]]',
   NOW(), NOW());
```

---

## RabbitMQ Setup

### Docker Installation

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3.12-management-alpine
```

### Management UI Access

```
URL: http://localhost:15672
Username: guest
Password: guest
```

### Verify Queue Setup

The application automatically creates the required queue and exchanges on startup:

```
Queue: notification.request.queue
Exchange: notification.request.exchange
Routing Key: notification.request.*
```

### Test Message Publishing (RabbitMQ CLI)

```bash
# Connect to RabbitMQ container
docker exec -it rabbitmq rabbitmqctl

# List queues
rabbitmqctl list_queues

# Publish test message
rabbitmq-publish --vhost / \
  --username guest --password guest \
  --exchange notification.request.exchange \
  --routing-key notification.request.test \
  '{"traceId":"test-1","recipient":"+6281234567890","slug":"welcome","language":"en","channel":"whatsapp","variables":{}}'
```

---

## Email Configuration

### Gmail Setup

1. **Enable 2-Factor Authentication**
   - Go to [Google Account Security](https://myaccount.google.com/security)
   - Enable 2-Step Verification

2. **Generate App Password**
   - Go to [App Passwords](https://myaccount.google.com/apppasswords)
   - Select "Mail" and "Windows Computer"
   - Copy the generated 16-character password

3. **Configuration**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: xxxx-xxxx-xxxx-xxxx  # 16-char app password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
```

### Custom SMTP Server

```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-username
    password: your-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.from: noreply@example.com
```

### SendGrid Integration

```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: ${SENDGRID_API_KEY}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

---

## WhatsApp Configuration

### Watzap.id Setup

1. **Register Account**
   - Visit [Watzap.id](https://watzap.id)
   - Create account and verify phone number

2. **Get API Credentials**
   - API Key: Found in account settings
   - Number Key: Associated with your WhatsApp number

3. **Configuration**
```yaml
watzap:
  api-key: your-api-key-here
  api-url: https://api.watzap.id/v1
  number-key: your-number-key-here
  connect-timeout: 5000
  read-timeout: 10000
```

### Test Connection

```bash
curl -X GET https://api.watzap.id/v1/status \
  -H "Authorization: Bearer your-api-key-here"
```

---

## Deployment

### Building JAR

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Output: target/notification-service-1.0.0.jar
```

### Running as JAR

```bash
java -jar notification-service-1.0.0.jar \
  --server.port=8080 \
  --spring.datasource.url=jdbc:postgresql://db-server:5432/notif_db \
  --spring.datasource.username=notif_user \
  --spring.datasource.password=secure_password
```

### Docker Deployment

**Dockerfile**
```dockerfile
FROM eclipse-temurin:21-jdk-alpine
COPY target/notification-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**docker-compose.yml** (Production)
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: notification_db
      POSTGRES_USER: notif_user
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U notif_user"]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 30s
      timeout: 10s
      retries: 5

  notification-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/notification_db
      DB_USERNAME: notif_user
      DB_PASSWORD: secure_password
      RABBITMQ_HOST: rabbitmq
      MAIL_HOST: ${MAIL_HOST}
      MAIL_PORT: ${MAIL_PORT}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      WATZAP_API_KEY: ${WATZAP_API_KEY}
      WATZAP_NUMBER_KEY: ${WATZAP_NUMBER_KEY}
      API_KEY_SECRET: ${API_KEY_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres_data:
  rabbitmq_data:
```

---

## Troubleshooting

### Database Connection Issues

```bash
# Test PostgreSQL connection
psql -h localhost -U notif_user -d notification_db

# Check connection string
jdbc:postgresql://localhost:5432/notification_db

# Verify credentials
# Check environment variables are set correctly
env | grep DB_
```

### RabbitMQ Connection Issues

```bash
# Test RabbitMQ connectivity
docker exec rabbitmq rabbitmq-diagnostics ping

# Check queue status
docker exec rabbitmq rabbitmqctl list_queues

# Reset RabbitMQ (if needed)
docker exec rabbitmq rabbitmqctl reset
docker exec rabbitmq rabbitmqctl start_app
```

### Mail Configuration Issues

```bash
# Test SMTP connection (telnet)
telnet smtp.gmail.com 587

# Verify credentials
curl -X POST --ssl-reqd \
  --url 'smtps://smtp.gmail.com:465' \
  --user 'your-email@gmail.com:app-password'
```

### Application Startup Issues

```bash
# Enable debug logging
export SPRING_PROFILES_ACTIVE=debug

# Check logs
tail -f logs/notification-service.log

# Verify port is available
lsof -i :8080

# Check environment variables
java -jar app.jar --list-config-properties | grep notif
```

### Performance Tuning

```yaml
# Increase thread pool
spring:
  task:
    execution:
      pool:
        core-size: 16
        max-size: 32
        queue-capacity: 1000

# Optimize database
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
  
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
          fetch_size: 100
```

---

## Monitoring

### Health Endpoints

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Database health
curl http://localhost:8080/actuator/health/db

# Mail health (custom)
curl http://localhost:8080/actuator/health/notificationHealth
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# All available metrics
curl http://localhost:8080/actuator/metrics
```

### Logging Best Practices

```yaml
# Structured logging
logging:
  pattern:
    json: '{"time":"%d","level":"%p","thread":"%t","logger":"%c","msg":"%m","traceId":"%X{traceId}"}%n'
  level:
    org.springframework: WARN
    org.springframework.amqp: INFO
    org.hibernate: WARN
```

