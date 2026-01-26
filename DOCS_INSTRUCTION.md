# ROLE: Senior Java Architect & Messaging Expert
Expert in Spring Boot 3.4+, Java 25, Domain-Driven Design (DDD), and Resilient Systems.

## 🎯 OBJECTIVE
Build a high-performance, asynchronous **Notification Service** to centralize WhatsApp (Watzap.id) and Email communications.

## 🛠 TECH STACK & STANDARDS
- **Java 25**: Utilize Records, Pattern Matching, and Unnamed Variables.
- **Spring Boot 3.4+**: Web, Data JPA, Mail, Actuator.
- **Database**: PostgreSQL (with JSONB support).
- **Architecture**: Domain-Driven Design (DDD).
- **Template Engine**: Thymeleaf (String Template Resolver for DB-based templates).
- **Reliability**: Asynchronous processing (@Async) & Task Tracing.
- **Testing**: TDD approach with JUnit 5 and Testcontainers (PostgreSQL).

## 🗄️ DATABASE SCHEMA (PostgreSQL)
Implement these tables using JPA Entities:

1. **notification_templates**:
   - `slug` & `language` (Composite PK)
   - `channel` (EMAIL, WHATSAPP)
   - `template_type` (TEXT, IMAGE)
   - `subject` (Nullable, for Email)
   - `content` (TEXT, contains Thymeleaf placeholders `[[${var}]]`)
   - `image_url` (Nullable, for WA Image)

2. **notification_logs**:
   - `id` (UUID, PK), `trace_id` (UUID)
   - `recipient`, `slug`, `channel`, `status` (PENDING, SUCCESS, FAILED)
   - `variables` (JSONB), `error_message` (TEXT), `created_at`.

## 🚀 CORE LOGIC FLOW
1. **Entry**: REST Controller receives `NotificationRequest`.
2. **Trace**: Service generates `trace_id` and saves log as `PENDING`.
3. **Async Execute**:
   - Fetch template from DB with **Language Fallback** logic (requested lang -> default 'en').
   - Render `content` using `StringTemplateResolver`.
   - **Email Adapter**: Uses `JavaMailSender`.
   - **WhatsApp Adapter**: Uses Watzap.id API (`/send_message` or `/send_image`).
4. **Finalize**: Update log to `SUCCESS` or `FAILED` with error details.

## 🛡️ CONSTRAINTS
- Every external API call must have **Timeout** (5s connect, 10s read).
- Use **MDC** to carry `trace_id` in application logs.
- Use **TDD**: Write a test case for the template rendering and fallback logic first.
- Provide a `/actuator/health` endpoint that checks DB and Mail status.
