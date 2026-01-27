# Swagger OpenAPI Documentation Implementation

## Overview
Successfully implemented Swagger UI documentation with API Key authentication for the Notification Service.

## What Was Added

### 1. **Dependency** (pom.xml)
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

### 2. **Configuration** (application.yml)
```yaml
springdoc:
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
    displayOperationId: false
    deepLinking: true
  api-docs:
    path: /v3/api-docs
  show-actuator: false
  use-fqn-for-parameter-name: true
```

### 3. **OpenAPI Configuration Class**
Created `OpenApiConfiguration.java` which:
- Defines API metadata (title, version, description, contact)
- Configures two server environments (Local Development & Production)
- Sets up API Key security scheme for X-API-Key header authentication
- Organizes APIs into two tags:
  - **Template Management**: Template CRUD operations
  - **Notification Engine**: Notification sending operations

### 4. **Annotated Controllers**

#### TemplateController
All CRUD endpoints are now fully documented with:
- `@Operation` - Clear operation summaries and descriptions
- `@ApiResponses` - Response code documentation (200, 201, 204, 400, 401, 404, 409)
- `@Parameter` - Parameter descriptions and examples
- `@SecurityRequirement` - Indicates which endpoints require API Key auth
- `@Tag` - Groups endpoints under "Template Management"

**Endpoints:**
- `POST /api/v1/templates` - Create template (requires API Key)
- `GET /api/v1/templates/{slug}/{lang}` - Fetch template
- `PUT /api/v1/templates/{slug}/{lang}` - Update template (requires API Key)
- `DELETE /api/v1/templates/{slug}/{lang}` - Delete template (requires API Key)

#### NotificationController
All endpoints are documented with:
- `@Operation` - Clear operation summaries and descriptions
- `@ApiResponses` - Response code documentation
- `@Tag` - Groups endpoints under "Notification Engine"

**Endpoints:**
- `POST /api/v1/notifications/send` - Send notification
- `GET /api/v1/notifications/health` - Health check

## How to Access

### Swagger UI
- **URL**: `http://localhost:8080/swagger-ui.html`
- **API Docs (JSON)**: `http://localhost:8080/v3/api-docs`

### Testing with API Key
1. Open Swagger UI in your browser
2. Look for the "Authorize" button in the top-right corner
3. Enter your API Key value (header: `X-API-Key`)
4. Protected endpoints will now include the API Key header automatically when testing

## Features

✅ Full API documentation with clear summaries and descriptions
✅ API Key authentication support with security scheme definition
✅ Response code documentation with content schema references
✅ Parameter examples and descriptions
✅ Endpoint grouping by functionality
✅ Server environment configuration
✅ Java 25 records properly rendered in schema
✅ Interactive testing via Swagger UI

## Building & Running

```bash
# Build project
mvn clean package -DskipTests

# Run application
java -jar target/notification-service-1.0.0.jar

# Or with Maven
mvn spring-boot:run
```

The application will start on port 8080, and you can immediately access the Swagger UI.
