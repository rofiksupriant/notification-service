# Local Development Setup

Set up your local development environment for the Notification Service.

## Prerequisites

- **Java 21+** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **PostgreSQL 16+** - [Download](https://www.postgresql.org/download/)
- **Docker** (optional, for PostgreSQL in container)
- **RabbitMQ** (optional, for async messaging)

## Option 1: Local Setup (Without Docker)

### 1. Clone Repository

```bash
git clone https://github.com/rofiksupriant/notification-service.git
cd notification-service
```

### 2. Create `.env` File

```bash
# Copy template
cp .env.example .env

# Edit with your values
nano .env
```

**Minimal configuration:**
```bash
NOTIF_DB_URL=jdbc:postgresql://localhost:5432/notif_db
NOTIF_DB_USERNAME=postgres
NOTIF_DB_PASSWORD=postgres
NOTIF_API_SECRET=your-secret-key
NOTIF_MAIL_USERNAME=your-email@gmail.com
NOTIF_MAIL_PASSWORD=app-specific-password
NOTIF_WATZAP_API_KEY=your-watzap-key
NOTIF_WATZAP_PHONE_ID=your-phone-id
```

### 3. Create Database

```bash
# Create database
createdb -U postgres notif_db

# Or via psql
psql -U postgres -c "CREATE DATABASE notif_db;"
```

### 4. Verify Connectivity

```bash
# Test connection
psql -U postgres -d notif_db -c "SELECT 1;"
```

### 5. Run Application

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Or from IDE
# In IntelliJ/VS Code: Click "Run" on main class
```

Application starts at: `http://localhost:8080`

---

## Option 2: Docker Compose Setup (Recommended)

### 1. Clone Repository

```bash
git clone https://github.com/rofiksupriant/notification-service.git
cd notification-service
```

### 2. Create `.env` File

```bash
cp .env.example .env
```

**For local Docker setup:**
```bash
NOTIF_DB_URL=jdbc:postgresql://notification-db:5432/notif_db
NOTIF_DB_USERNAME=postgres
NOTIF_DB_PASSWORD=postgres
NOTIF_API_SECRET=dev-secret
NOTIF_MAIL_USERNAME=your-email@gmail.com
NOTIF_MAIL_PASSWORD=app-password
NOTIF_WATZAP_API_KEY=your-key
NOTIF_WATZAP_PHONE_ID=your-phone
```

### 3. Start Services

```bash
# Go to docker directory
cd docker

# Start all services
docker-compose up --build

# Or detached (background)
docker-compose up -d --build
```

### 4. Verify Services

```bash
# Check running containers
docker-compose ps

# View logs
docker-compose logs -f notification-service
```

Application starts at: `http://localhost:8080`

### 5. Access Database (if needed)

```bash
# Connect to PostgreSQL
docker-compose exec notification-db psql -U postgres -d notif_db

# List tables
\dt

# Exit
\q
```

---

## Quick Commands

### Run Application

```bash
# Using Maven
mvn spring-boot:run

# Using Docker Compose (from docker directory)
docker-compose up --build
```

### Run Tests

```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=NotificationIntegrationTest

# Skip tests (faster build)
mvn clean install -DskipTests
```

### View Logs

```bash
# Application console (Maven)
# Logs appear in terminal

# Docker
docker-compose logs -f notification-service

# See last 50 lines
docker-compose logs --tail=50 notification-service
```

### Access API Documentation

```
http://localhost:8080/swagger-ui.html
```

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

---

## Troubleshooting

### Database Connection Fails

```bash
# Verify PostgreSQL is running
psql -U postgres -c "SELECT 1;"

# Or check Docker container
docker-compose ps

# View PostgreSQL logs
docker-compose logs notification-db
```

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080  # Linux/Mac

# Kill process
kill -9 <PID>

# Or change port in application.yml
NOTIF_SERVER_PORT=9090
```

### Maven Build Fails

```bash
# Clean cache
mvn clean

# Update dependencies
mvn dependency:resolve

# Check Java version
java -version
```

### Template Rendering Issues

```bash
# Check template files exist
ls src/main/resources/templates/

# Verify template syntax
# Uses Thymeleaf: [[${variableName}]]
```

---

## IDE Setup

### IntelliJ IDEA

1. **Import Project**
   - File → Open
   - Select project root
   - Import as Maven project

2. **Configure Run**
   - Run → Edit Configurations
   - Add new "Maven" configuration
   - Command: `spring-boot:run`
   - Click "Run"

3. **Environment Variables**
   - View → Tool Windows → Environment Variables
   - Or create `application-local.yml`

### VS Code

1. **Install Extensions**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - REST Client

2. **Run Application**
   - Command Palette (Ctrl+Shift+P)
   - "Java: Run" or "Spring Boot: Start"

3. **Debug**
   - Set breakpoint (click left margin)
   - Run in Debug mode
   - Step through code

---

## Environment Variables

See [Environment Variables Reference](../reference/ENVIRONMENT.md) for complete list.

**Most Important:**
- `NOTIF_DB_PASSWORD` - Database password
- `NOTIF_API_SECRET` - API security key
- `NOTIF_MAIL_USERNAME` - Email address
- `NOTIF_MAIL_PASSWORD` - Email app password
- `NOTIF_WATZAP_API_KEY` - WhatsApp API key

---

## Next Steps

1. ✅ Run application locally
2. ✅ Test API via [Swagger UI](http://localhost:8080/swagger-ui.html)
3. ✅ Read [README.md](../../README.md) for features
4. ✅ Review [Architecture Guide](../ARCHITECTURE.md)
5. ✅ Run tests: `mvn test`

---

## Need Help?

- 🐳 [Docker Setup Guide](../docker/OVERVIEW.md) - Container questions
- 🔧 [Troubleshooting](../reference/TROUBLESHOOTING.md) - Common issues
- 📚 [Environment Variables](../reference/ENVIRONMENT.md) - Configuration help
- 🏗️ [Architecture Guide](../ARCHITECTURE.md) - System design

**Happy coding!** 🚀
