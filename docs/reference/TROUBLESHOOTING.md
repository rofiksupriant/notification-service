# Troubleshooting Guide

Common issues and their solutions.

---

## Application Won't Start

### Problem: "Connection refused" when trying to access http://localhost:8080

**Possible Causes:**
1. Application didn't start
2. Wrong port
3. Application crashed

**Solutions:**

```bash
# Check if application is running
docker ps | grep notification-service

# View logs to see error
docker compose logs notification-service

# Common errors in logs:
# - "Database connection failed"
# - "Port 8080 already in use"
# - "Failed to start"
```

**If logs show database error:**
```bash
# Verify database is running
docker compose ps | grep notification-db

# Check database connection
docker compose exec notification-service \
  pg_isready -h notification-db -p 5432
```

**If logs show port in use:**
```bash
# Find what's using port 8080
lsof -i :8080  # Linux/Mac
Get-NetTCPConnection -LocalPort 8080  # Windows

# Either kill the process or change port in docker-compose.prod.yml
# ports: - "9090:8080"  # Use 9090 instead
```

---

## Docker Build Fails

### Problem: `docker build` command fails

**Possible Causes:**
1. Maven build failure
2. Missing Dockerfile
3. Insufficient disk space
4. Docker daemon not running

**Solutions:**

```bash
# Verify Docker is running
docker ps

# If Docker not running:
# - Windows: Start "Docker Desktop"
# - Mac: Start Docker.app
# - Linux: sudo systemctl start docker

# Check disk space
df -h              # Linux/Mac
Get-Volume         # Windows
# Need 5GB+ free space

# Verify Dockerfile exists
ls -l Dockerfile

# Build with verbose output
docker build -t myapp:1.0.0 . --progress=plain

# Check Maven compilation first
mvn clean install -DskipTests
```

**If Maven fails:**
```bash
# Check Java version
java -version
# Need Java 21+

# Check Maven version
mvn --version
# Need Maven 3.9+

# Clean Maven cache
mvn clean
rm -rf ~/.m2/repository  # Linux/Mac

# Update dependencies
mvn dependency:resolve
```

---

## Docker Image Push Fails

### Problem: `docker push` fails or image not found on Docker Hub

**Possible Causes:**
1. Not logged in to Docker Hub
2. Image name doesn't match username
3. Network connection issue

**Solutions:**

```bash
# Check Docker Hub login
docker login
# Enter username and password

# Verify login succeeded
docker info | grep -i username

# Check image exists locally
docker images | grep notification-service

# Tag image correctly
docker tag local-image:tag your_username/notification-service:1.0.0

# Push again
docker push your_username/notification-service:1.0.0

# Verify on Docker Hub
# Visit: https://hub.docker.com/r/your_username/notification-service
```

---

## Database Connection Errors

### Problem: Application can't connect to database

**Error Messages:**
- "Connection refused"
- "FATAL: password authentication failed"
- "database does not exist"

**Solutions:**

```bash
# Verify database is running
docker compose ps

# Check database is healthy
docker compose ps | grep notification-db
# Should show "Up (healthy)"

# Wait for database to be ready (can take 30+ seconds)
docker compose logs notification-db | tail -20

# Test database connection directly
docker compose exec notification-service \
  pg_isready -h notification-db -p 5432

# Check database credentials in .env
grep "^DB_" .env

# Verify database exists
docker compose exec notification-db \
  psql -U postgres -d notif_db -c "SELECT 1;"
```

**If "password authentication failed":**
```bash
# Check password in .env
grep DB_PASSWORD .env

# Verify it matches in docker-compose.prod.yml
grep POSTGRES_PASSWORD docker/docker-compose.prod.yml

# They must be identical

# If using Docker, recreate database with correct password
docker compose down -v  # Remove volume (careful - loses data!)
docker compose up -d
```

**If "database does not exist":**
```bash
# Database is created automatically by Docker

# If missing, check Flyway migrations
docker compose logs notification-db | grep -i "migration\|flyway"

# Can recreate:
docker compose down -v
docker compose up -d
```

---

## Email Not Sending

### Problem: Email notifications not being sent

**Possible Causes:**
1. Credentials wrong
2. Gmail app password issue
3. Network blocked
4. SMTP not configured

**Solutions:**

```bash
# Check email configuration
grep "^NOTIF_MAIL" .env

# Verify email is enabled
grep "NOTIF_MAIL_ENABLED" .env
# Should be "true"

# Check logs for mail errors
docker compose logs notification-service | grep -i "mail\|email"

# Verify Gmail app password (NOT regular password)
# 1. Visit: https://myaccount.google.com/apppasswords
# 2. Should be 16 characters with spaces: "xxxx xxxx xxxx xxxx"
# 3. Paste exactly as given (with spaces)

# Test SMTP connection manually
docker compose exec notification-service \
  bash -c 'echo | telnet smtp.gmail.com 587'
```

**If connection times out:**
```bash
# Network might be blocking SMTP

# Try different port
# 587 (TLS) - Standard
# 465 (SSL) - Alternative

# Update docker-compose.prod.yml
NOTIF_MAIL_PORT=465

# Or check with your IT/Network team
# Some networks block outbound SMTP
```

**If "Authentication failed":**
```bash
# Gmail app password is wrong

# 1. Go to: https://myaccount.google.com/apppasswords
# 2. Generate NEW app password
# 3. Copy and paste exactly (keep spaces)
# 4. Update .env: NOTIF_MAIL_PASSWORD=xxxx xxxx xxxx xxxx
# 5. Restart container: docker compose restart notification-service
```

---

## WhatsApp Not Sending

### Problem: WhatsApp notifications not delivered

**Possible Causes:**
1. API key invalid
2. Phone ID wrong
3. WhatsApp account not activated
4. Network issue

**Solutions:**

```bash
# Check WhatsApp configuration
grep "^NOTIF_WATZAP" .env

# Verify WhatsApp is enabled
grep "NOTIF_WATZAP_ENABLED" .env
# Should be "true"

# Check logs for WhatsApp errors
docker compose logs notification-service | grep -i "watzap\|whatsapp"

# Verify credentials on Watzap dashboard
# 1. Visit: https://watzap.id
# 2. Login to account
# 3. Check API Key
# 4. Check Phone ID
# 5. Verify account is active

# Test API key format
# API Key should look like: "abc123def456ghi789"
# Phone ID should be number: "1234567890"

# Update credentials if needed
nano .env
# Edit NOTIF_WATZAP_API_KEY and NOTIF_WATZAP_PHONE_ID
# Save and restart
docker compose restart notification-service
```

---

## Port Already in Use

### Problem: "Address already in use" when starting container

**Possible Causes:**
1. Port 8080 is being used by another application
2. Old container still running
3. Docker compose still running

**Solutions:**

```bash
# Find process using port 8080
lsof -i :8080              # Linux/Mac
Get-NetTCPConnection -LocalPort 8080  # Windows

# Kill the process
kill -9 PID                # Linux/Mac
Stop-Process -Id PID -Force  # Windows

# Or use different port in docker-compose.prod.yml
# Change: ports: - "8080:8080"
# To:     ports: - "9090:8080"

# Verify port is free
lsof -i :9090  # Should return nothing

# Start container with new port
docker compose up -d
```

---

## Container Keeps Restarting

### Problem: Container restarts repeatedly

**Possible Causes:**
1. Application crashes on startup
2. Configuration error
3. Database not ready
4. Resource limit

**Solutions:**

```bash
# Check logs for crash reason
docker compose logs notification-service

# Look for:
# - Exception messages
# - "Connection refused"
# - "OutOfMemory"
# - Port conflicts

# Get last 50 lines of logs
docker compose logs --tail=50 notification-service

# If database error, wait longer
# Database can take 30+ seconds to start

# If always crashes, check .env
grep -v "^#" .env | grep "^[A-Z]"

# Verify all required variables are set
nano .env

# Check resource limits (if set)
docker stats notification-service
```

---

## Health Check Failing

### Problem: Health endpoint shows "DOWN" or "UNHEALTHY"

**Error:**
```
"status":"DOWN"
"components":{"db":{"status":"DOWN"}}
```

**Solutions:**

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Check application logs
docker compose logs notification-service | tail -30

# Check database health
curl http://localhost:8080/actuator/health/db

# Verify database connection
docker compose exec notification-service \
  pg_isready -h notification-db -p 5432

# If database is down
docker compose restart notification-db

# Wait 30 seconds and try again
sleep 30
curl http://localhost:8080/actuator/health
```

---

## Can Connect to Database from Host (Security Issue)

### Problem: Can access PostgreSQL from host/internet (should NOT be possible!)

**Correct behavior:** Database NOT accessible from host

**Problem behavior:** Can connect with:
```bash
psql -h localhost -U postgres -d notif_db
```

**Solutions:**

```bash
# Check docker-compose.prod.yml
grep -A5 "postgres:" docker-compose.prod.yml

# Should show NO "ports" line for postgres service

# If ports are defined:
# ports:
#   - "5432:5432"

# REMOVE these lines! Database should only be internal

# Correct configuration:
# postgres:
#   image: postgres:17-alpine
#   # NO ports line

# Verify fix
docker compose down
docker compose up -d

# Try to connect (should fail)
psql -h localhost -U postgres -d notif_db
# ERROR: connection refused (correct!)
```

---

## Logs Are Too Verbose

### Problem: Too many logs or wrong log level

**Solutions:**

```bash
# Check current log level
grep "NOTIF_LOGGING" .env

# For production, use INFO level
NOTIF_LOGGING_LEVEL_APP=INFO      # Not DEBUG
NOTIF_LOGGING_LEVEL_ROOT=INFO     # Not TRACE

# Don't log SQL queries in production
SPRING_JPA_SHOW_SQL=false         # Not true

# View last N lines only
docker compose logs --tail=20 notification-service

# View logs since specific time
docker compose logs --since 2024-01-31T10:00:00

# Clear old logs
docker logs --tail 0 notification-service > /dev/null

# Update .env and restart
docker compose restart notification-service
```

---

## Out of Disk Space

### Problem: Docker run fails with "No space left on device"

**Solutions:**

```bash
# Check disk usage
df -h              # Linux/Mac
Get-Volume         # Windows

# Clean up Docker images and containers
docker system prune -a

# Remove unused volumes
docker volume prune

# Check size of specific image
docker images --format "{{.Repository}} {{.Size}}"

# Remove old images
docker rmi image_id

# Check container logs size (can be large!)
docker logs --tail 0 container_id > /dev/null
```

---

## Network Issues

### Problem: Container can't reach external services

**Solutions:**

```bash
# Test network connectivity
docker compose exec notification-service ping 8.8.8.8

# Test DNS resolution
docker compose exec notification-service nslookup gmail.com

# Check network configuration
docker network inspect app-network

# If containers can't communicate
docker compose down
docker compose up -d

# Test inter-container communication
docker compose exec notification-service ping notification-db
```

---

## Configuration Issues

### Problem: "Unknown variable" or "property not found"

**Solutions:**

```bash
# Verify variable name format
# Correct: NOTIF_API_SECRET
# Wrong: NOTIF_API_SECRET= (trailing =)

# Check for typos
grep "NOTIF_" .env | sort

# Compare with documentation
# See: docs/reference/ENVIRONMENT.md

# Remove spaces around =
# Correct: KEY=value
# Wrong: KEY = value

# Update .env and restart
docker compose restart notification-service
```

---

## Performance Issues

### Problem: Application is slow or unresponsive

**Solutions:**

```bash
# Check resource usage
docker stats notification-service

# If high CPU or memory
docker compose logs notification-service | grep -i "memory\|cpu"

# Check database performance
docker compose exec notification-db \
  psql -U postgres -d notif_db -c "SELECT count(*) FROM notifications;"

# Increase Tomcat threads if needed
SERVER_TOMCAT_THREADS_MAX=300

# Increase connection pool if needed
NOTIF_DB_HIKARI_MAX_POOL_SIZE=20

# Update .env and restart
docker compose restart notification-service
```

---

## Testing Endpoints

### Verify Application is Working

```bash
# Health check
curl -s http://localhost:8080/actuator/health | jq .

# Info endpoint
curl -s http://localhost:8080/actuator/info | jq .

# Swagger UI
curl -s http://localhost:8080/swagger-ui.html | head -20

# Send test notification
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "slug": "test-template",
    "recipient": "test@example.com",
    "channel": "EMAIL",
    "language": "en",
    "variables": {"userName": "Test User"}
  }'
```

---

## Still Having Issues?

### Get Help

1. **Check logs first:**
   ```bash
   docker compose logs -f notification-service
   ```

2. **Review documentation:**
   - [Environment Variables](ENVIRONMENT.md) - Configuration
   - [Docker Commands](COMMANDS.md) - Command reference
   - [Deployment Workflow](../deployment/WORKFLOW.md) - Step-by-step

3. **Search for error message** in this troubleshooting guide

4. **Check application logs** for full error details

5. **Ask for help** with:
   - Error message (exact text)
   - Your configuration (.env values, redact sensitive)
   - Steps to reproduce
   - Docker version, OS

---

**Last Updated:** January 31, 2026  
**Status:** ✅ Comprehensive troubleshooting guide
