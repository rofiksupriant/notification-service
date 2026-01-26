notification-service/
│
├── 📄 DOCS_INSTRUCTION.md              # Architecture guidelines (refer anytime)
├── 📄 README.md                        # Complete documentation
├── 📄 QUICKSTART.md                    # 5-minute setup guide
├── 📄 IMPLEMENTATION_SUMMARY.md        # What was built
├── 📄 .gitignore                       # Git ignore patterns
├── 📄 pom.xml                          # Maven: Java 25, Spring Boot 3.4+
│
├── 📁 docker/
│   └── docker-compose.yml              # PostgreSQL 16 container
│
├── 📁 src/main/java/com/vibe/notification/
│   │
│   ├── NotificationServiceApplication.java  # Spring Boot entry point
│   │
│   ├── 📁 domain/                      # 🏛️ DDD: Business Logic
│   │   ├── model/
│   │   │   ├── Channel.java            # ✅ EMAIL, WHATSAPP enum
│   │   │   ├── TemplateType.java       # ✅ TEXT, IMAGE enum
│   │   │   ├── NotificationStatus.java # ✅ PENDING, SUCCESS, FAILED enum
│   │   │   └── NotificationRequest.java # ✅ Java 25 Record
│   │   ├── service/
│   │   │   ├── TraceService.java       # ✅ MDC trace_id management
│   │   │   ├── TemplateResolutionService.java  # ✅ Language fallback logic
│   │   │   ├── TemplateRenderingService.java   # ✅ Thymeleaf rendering
│   │   │   └── NotificationDomainService.java  # ✅ Log lifecycle
│   │   └── exception/
│   │       ├── NotificationException.java
│   │       ├── TemplateNotFoundException.java
│   │       └── TemplateRenderingException.java
│   │
│   ├── 📁 application/                 # 🎯 DDD: Use Cases
│   │   ├── dto/
│   │   │   ├── SendNotificationRequest.java   # ✅ API request
│   │   │   └── NotificationResponse.java      # ✅ API response
│   │   └── NotificationApplicationService.java # ✅ Main orchestrator
│   │
│   ├── 📁 infrastructure/              # 🔧 DDD: Infrastructure
│   │   ├── persistence/
│   │   │   ├── entity/
│   │   │   │   ├── NotificationTemplateEntity.java    # ✅ JPA entity
│   │   │   │   ├── NotificationTemplateId.java        # ✅ Composite key
│   │   │   │   └── NotificationLogEntity.java         # ✅ JPA entity with JSONB
│   │   │   └── repository/
│   │   │       ├── NotificationTemplateRepository.java # ✅ Data access
│   │   │       └── NotificationLogRepository.java      # ✅ Data access
│   │   ├── adapter/
│   │   │   ├── email/
│   │   │   │   ├── EmailNotificationAdapter.java      # ✅ JavaMail integration
│   │   │   │   └── EmailProperties.java               # ✅ SMTP config
│   │   │   └── whatsapp/
│   │   │       └── WhatsAppNotificationAdapter.java   # ✅ Watzap integration
│   │   └── external/
│   │       └── watzap/
│   │           ├── WatzapClient.java          # ✅ HTTP client (timeout + retry)
│   │           ├── WatzapResponse.java        # ✅ Java 25 Record
│   │           └── WatzapProperties.java      # ✅ Config
│   │
│   ├── 📁 presentation/                # 💻 DDD: REST API
│   │   ├── controller/
│   │   │   ├── NotificationController.java            # ✅ REST endpoints
│   │   │   └── NotificationHealthIndicator.java       # ✅ Custom health checks
│   │   └── error/
│   │       └── GlobalExceptionHandler.java            # ✅ Error handling
│   │
│   └── 📁 config/                      # ⚙️ Spring Configuration
│       ├── ThymeleafConfig.java        # ✅ StringTemplateResolver
│       └── AsyncConfig.java            # ✅ @EnableAsync
│
├── 📁 src/main/resources/
│   ├── application.yml                 # ✅ Production config
│   ├── application-test.yml            # ✅ Test config
│   └── db/migration/                   # ✅ Flyway migrations
│       ├── V1__Create_notification_templates.sql
│       └── V2__Create_notification_logs.sql
│
├── 📁 src/test/java/com/vibe/notification/
│   ├── domain/service/
│   │   ├── TemplateResolutionServiceTest.java  # ✅ 3 tests (language fallback)
│   │   └── TemplateRenderingServiceTest.java   # ✅ 4 tests (Thymeleaf)
│   └── integration/
│       └── NotificationIntegrationTest.java    # ✅ 6 E2E tests (Testcontainers)
│
└── 📁 logs/                            # Application logs (generated at runtime)
    └── notification-service.log


═══════════════════════════════════════════════════════════════════════════════

ARCHITECTURE OVERVIEW
═════════════════════

REST API Request
      ↓
[NotificationController] ── accepts SendNotificationRequest
      ↓
[NotificationApplicationService]
      ├─ generates trace_id (MDC)
      ├─ creates PENDING log via NotificationDomainService
      ├─ returns 202 Accepted immediately
      └─ triggers @Async processNotificationAsync()
            ↓
      [Background Thread]
      ├─ TemplateResolutionService.resolveTemplate(slug, language)
      │    └─ Fallback: requested → en (if not found)
      ├─ TemplateRenderingService.renderContent(template, variables)
      │    └─ Thymeleaf StringTemplateResolver
      ├─ EmailNotificationAdapter OR WhatsAppNotificationAdapter
      │    ├─ Email: JavaMailSender (5s connect, 10s read timeout)
      │    └─ WhatsApp: WatzapClient (5s connect, 10s read timeout + 2x retry)
      └─ NotificationDomainService.markAsSent() or markAsFailed()
            └─ Log updated to SUCCESS or FAILED
            
Distributed Tracing: trace_id in MDC → all logs include [traceId=xxx]
Database: All events logged to notification_logs with JSONB variables

═══════════════════════════════════════════════════════════════════════════════

KEY IMPLEMENTATION DETAILS
══════════════════════════

✅ DOMAIN-DRIVEN DESIGN
   • Domain Layer: Business logic (services, entities, value objects)
   • Application Layer: Use cases (orchestration, transactions)
   • Infrastructure Layer: Persistence, external APIs, adapters
   • Presentation Layer: REST API, error handling

✅ JAVA 25 FEATURES
   • Records: NotificationRequest, WatzapResponse, SendNotificationRequest
   • Pattern Matching: Channel/TemplateType/NotificationStatus enums with switch
   • Unnamed Variables: Use _ in patterns where value is unused

✅ SPRING BOOT 3.4+ FEATURES
   • Spring Data JPA: Template/Log repositories with custom queries
   • Spring Mail: JavaMailSender for Email
   • Spring WebFlux: WebClient for Watzap.id API with timeout
   • Spring Actuator: Health checks, metrics, logging management
   • @Async: Asynchronous processing with thread pool executor

✅ DATABASE DESIGN
   • PostgreSQL 16 with JSONB support
   • Composite key (slug, language) for templates
   • UUID primary key for logs
   • Indexes on trace_id, recipient, status, created_at for performance
   • Flyway migrations for schema versioning

✅ RESILIENCE & RELIABILITY
   • Timeout: 5s connect, 10s read on external APIs
   • Retry: 2x exponential backoff (500ms) on Watzap.id
   • Fallback: Language fallback chain (id → en)
   • Health Checks: DB and Mail connectivity verification
   • Error Tracking: Complete error messages in logs

✅ DISTRIBUTED TRACING
   • MDC (Mapped Diagnostic Context) for trace_id
   • Automatic trace_id generation and propagation
   • All logs include [traceId=xxx] for request correlation
   • Async processing maintains trace_id context

✅ MULTI-CHANNEL SUPPORT
   • Email: Subject + Content rendering via JavaMail
   • WhatsApp: Text messages and Image+Caption via Watzap.id
   • Template types: TEXT and IMAGE

✅ TESTING STRATEGY
   • Unit Tests: Business logic with Mockito
   • Integration Tests: Full flow with Testcontainers + PostgreSQL
   • Async Tests: Awaitility for async assertion handling
   • Test Isolation: Database cleanup between tests

═══════════════════════════════════════════════════════════════════════════════

DATABASE SCHEMA
═══════════════

TABLE: notification_templates
├── slug (PK)         VARCHAR(50)
├── language (PK)     VARCHAR(5)
├── channel           VARCHAR(20)  -- EMAIL, WHATSAPP
├── template_type     VARCHAR(20)  -- TEXT, IMAGE
├── subject           VARCHAR(255) -- Optional
├── content           TEXT         -- Thymeleaf: [[${var}]]
├── image_url         TEXT         -- Optional
├── created_at        TIMESTAMP
└── updated_at        TIMESTAMP

TABLE: notification_logs
├── id (PK)           UUID
├── trace_id          UUID         -- Correlation ID
├── recipient         VARCHAR(100)
├── slug              VARCHAR(50)
├── channel           VARCHAR(20)
├── variables         JSONB        -- Flexible data storage
├── status            VARCHAR(20)  -- PENDING, SUCCESS, FAILED
├── error_message     TEXT         -- Failure details
├── sent_at           TIMESTAMP
└── created_at        TIMESTAMP

Indexes: trace_id, recipient, status, created_at

═══════════════════════════════════════════════════════════════════════════════

API EXAMPLES
════════════

1. SEND EMAIL NOTIFICATION
   curl -X POST http://localhost:8080/api/v1/notifications/send \
     -H "Content-Type: application/json" \
     -d '{
       "recipient": "user@example.com",
       "slug": "welcome",
       "language": "en",
       "channel": "EMAIL",
       "variables": {"name": "John", "company": "VibeCoding"}
     }'

2. SEND WHATSAPP MESSAGE
   curl -X POST http://localhost:8080/api/v1/notifications/send \
     -H "Content-Type: application/json" \
     -d '{
       "recipient": "+6281234567890",
       "slug": "otp",
       "language": "en",
       "channel": "WHATSAPP",
       "variables": {"code": "123456"}
     }'

3. CHECK HEALTH
   curl http://localhost:8080/api/v1/notifications/health
   curl http://localhost:8080/actuator/health

═══════════════════════════════════════════════════════════════════════════════

QUICK START
═══════════

1. Start PostgreSQL
   docker-compose -f docker/docker-compose.yml up -d

2. Set environment variables
   export DB_USERNAME=notif_user
   export DB_PASSWORD=notif_pass
   export MAIL_USERNAME=your-email@gmail.com
   export MAIL_PASSWORD=your-app-password
   export WATZAP_API_KEY=your-key
   export WATZAP_NUMBER_KEY=your-key

3. Run application
   mvn spring-boot:run

4. Insert template
   psql -U notif_user -d notif_db -c "
   INSERT INTO notification_templates (slug, language, channel, template_type, subject, content)
   VALUES ('welcome', 'en', 'EMAIL', 'TEXT', 'Welcome', 'Hello [[${name}]]!');"

5. Send notification
   curl -X POST http://localhost:8080/api/v1/notifications/send ...

═══════════════════════════════════════════════════════════════════════════════

TESTING
═══════

Run all tests:              mvn test
Run unit tests:             mvn test -Dtest=*ServiceTest
Run integration tests:      mvn test -Dtest=NotificationIntegrationTest

═══════════════════════════════════════════════════════════════════════════════

DEPLOYMENT CHECKLIST
════════════════════

□ All environment variables configured
□ PostgreSQL database created and accessible
□ Flyway migrations run successfully
□ Health check passes: GET /actuator/health
□ Template inserted for testing
□ Send test notification and verify in DB
□ Logs are being written to logs/notification-service.log
□ Async processing working (check notification_logs status updates)
□ Email/WhatsApp credentials verified
□ API timeout and retry settings verified

═══════════════════════════════════════════════════════════════════════════════

DOCUMENTS INCLUDED
══════════════════

1. DOCS_INSTRUCTION.md      - Architecture & requirements (refer anytime)
2. README.md                - Complete documentation & reference
3. QUICKSTART.md            - 5-minute setup guide
4. IMPLEMENTATION_SUMMARY.md - What was built
5. PROJECT_STRUCTURE.md     - This file

═══════════════════════════════════════════════════════════════════════════════

STATUS: ✅ PRODUCTION-READY

All components implemented and tested. Ready for deployment.

Last Updated: January 26, 2026
