# Production Deployment Workflow

Complete step-by-step guide for deploying to production.

**Time:** ~30 minutes | **Difficulty:** Intermediate

---

## Overview

```
┌─────────────┐    ┌──────────┐    ┌─────────┐    ┌────────┐    ┌──────────┐
│   BUILD     │ → │   PUSH   │ → │ PREPARE │ → │ CONFIG │ → │ DEPLOY   │
│             │    │          │    │  SERVER │    │        │    │          │
└─────────────┘    └──────────┘    └─────────┘    └────────┘    └──────────┘
   5 minutes       Docker Hub       Create dir    Copy files    Start app
```

---

## Phase 1: Build & Push (5 minutes)

### Step 1a: Verify Prerequisites

```powershell
# Windows - PowerShell
docker --version
mvn --version

# Should show Docker 20+ and Maven 3.9+
```

### Step 1b: Prepare Credentials

Create a Docker Hub account if needed:
- Visit: https://hub.docker.com
- Create account
- Note your username

### Step 1c: Login to Docker Hub

```powershell
# Windows
docker login

# Enter username and password
# Username: your_docker_username
# Password: your_docker_hub_password

# Verify login
docker info | Select-String "Username"
```

### Step 1d: Build & Push Image

```powershell
# Navigate to project root
cd C:\Path\To\notification-service

# Run build script (Windows)
.\build-and-push.ps1 your_docker_username 1.0.0

# What happens:
# 1. mvn clean install -DskipTests
# 2. docker build -t your_docker_username/notification-service:1.0.0 .
# 3. docker push your_docker_username/notification-service:1.0.0
# 4. docker push your_docker_username/notification-service:latest

# Expected output:
# ✓ Docker found
# ✓ Maven found
# ✓ Project compiled successfully
# ✓ Docker image built successfully
# ✓ Image verified: Size = 548MB
# ✓ Image pushed successfully
# ✓ Latest tag pushed successfully
```

### Step 1e: Verify on Docker Hub

1. Visit: https://hub.docker.com/r/your_docker_username/notification-service
2. Verify tags exist:
   - `1.0.0`
   - `latest`
3. Check image size: ~548 MB

---

## Phase 2: Prepare Server (5 minutes)

### Step 2a: SSH to Server

```bash
# SSH as root or sudoer
ssh user@your_server_ip

# Or with SSH key
ssh -i /path/to/key.pem user@your_server_ip
```

### Step 2b: Create Directory Structure

```bash
# Create application directory
sudo mkdir -p /opt/notification-service
cd /opt/notification-service

# Set permissions
sudo chown user:user /opt/notification-service
chmod 755 /opt/notification-service
```

---

## Phase 3: Copy Configuration Files (3 minutes)

### Step 3a: From Local Machine

Copy files from your development machine to server:

```bash
# Assuming SSH access (run from your machine)

# Copy docker-compose configuration
scp docker/docker-compose.prod.yml user@your_server_ip:/opt/notification-service/

# Copy environment template
scp .env.example user@your_server_ip:/opt/notification-service/.env
```

### Step 3b: Verify Files on Server

```bash
# On server
cd /opt/notification-service
ls -la

# Should see:
# docker-compose.prod.yml
# .env
```

---

## Phase 4: Configure Environment (5 minutes)

### Step 4a: Edit Configuration

On your server:

```bash
# Open environment file
nano .env

# Or use vi/vim
vim .env
```

### Step 4b: Set Required Values

Edit these values in `.env`:

```bash
# ========== DOCKER ==========
DOCKER_USERNAME=your_docker_username        # From step 1
APP_VERSION=1.0.0                          # Must match step 1

# ========== DATABASE ==========
DB_HOST=notification-db                    # Keep as is (internal)
DB_PORT=5432                               # Keep as is
DB_NAME=notif_db                           # Keep as is
DB_USER=postgres                           # Keep as is
DB_PASSWORD=YourVeryStrongPassword123!     # CHANGE THIS (16+ chars)

# ========== APPLICATION ==========
NOTIF_API_SECRET=YourRandomAPISecret       # Generate: openssl rand -base64 32

# ========== EMAIL ==========
NOTIF_MAIL_ENABLED=true                    # Enable email
NOTIF_MAIL_HOST=smtp.gmail.com             # Gmail SMTP
NOTIF_MAIL_PORT=587                        # Standard TLS
NOTIF_MAIL_USERNAME=your_email@gmail.com   # Your Gmail
NOTIF_MAIL_PASSWORD=xxxx xxxx xxxx xxxx    # Gmail App Password (NOT regular password!)
NOTIF_MAIL_FROM=noreply@yourcompany.com    # From address

# ========== WHATSAPP ==========
NOTIF_WATZAP_ENABLED=true                  # Enable WhatsApp
NOTIF_WATZAP_API_KEY=your_watzap_key       # From Watzap dashboard
NOTIF_WATZAP_PHONE_ID=your_phone_id        # From Watzap dashboard

# ========== LOGGING ==========
LOG_LEVEL=INFO                             # Production: INFO or WARN
SPRING_JPA_SHOW_SQL=false                  # Don't log SQL

# ========== SERVER ==========
SERVER_PORT=8080                           # Keep as is
SERVER_SERVLET_CONTEXT_PATH=/              # Keep as is
```

### Step 4c: Save Configuration

```bash
# Exit nano
Ctrl + X
# Answer: Y (yes to save)
# Confirm filename: .env
# Press Enter
```

### Step 4d: Secure Configuration

```bash
# Make .env readable only by owner
chmod 600 .env

# Verify permissions
ls -l .env
# Should show: -rw------- (600)
```

---

## Phase 5: Deploy Application (5 minutes)

### Step 5a: Start Services

On your server:

```bash
# Navigate to application directory
cd /opt/notification-service

# Start services
docker compose -f docker-compose.prod.yml --env-file .env up -d

# What happens:
# 1. Docker pulls image: your_docker_username/notification-service:1.0.0
# 2. Creates notification-service container
# 3. Pulls postgres:17-alpine image
# 4. Creates notification-db container
# 5. Connects them via internal network
# 6. Starts health checks
```

### Step 5b: Monitor Startup (30 seconds)

```bash
# Watch containers start
docker compose -f docker-compose.prod.yml ps

# Expected output:
# NAME                 STATUS         PORTS
# notification-service Up (starting)  0.0.0.0:8080->8080/tcp
# notification-db      Up (healthy)   

# Wait for "Up (healthy)"
# Takes ~30 seconds
```

### Step 5c: Check Logs

```bash
# View last 50 lines of logs
docker compose -f docker-compose.prod.yml logs --tail=50 notification-service

# Expected output (no errors):
# 2026-01-31 10:15:23.123 INFO [main] Started NotificationServiceApplication
# 2026-01-31 10:15:24.456 INFO [main] Tomcat started on port 8080
# 2026-01-31 10:15:25.789 INFO [main] Application ready to serve requests
```

---

## Phase 6: Verify Deployment (5 minutes)

### Step 6a: Check Container Status

```bash
# List all containers
docker compose -f docker-compose.prod.yml ps

# Expected output:
# NAME                 STATUS         PORTS
# notification-service Up (healthy)   0.0.0.0:8080->8080/tcp
# notification-db      Up (healthy)   (no exposed port)
```

### Step 6b: Test Health Endpoint

```bash
# Direct test (on server)
curl -s http://localhost:8080/actuator/health | python -m json.tool

# Or from your machine
curl -s http://your_server_ip:8080/actuator/health | python -m json.tool

# Expected response:
{
    "status": "UP",
    "components": {
        "db": {
            "status": "UP"
        },
        "livenessState": {
            "status": "UP"
        },
        "readinessState": {
            "status": "UP"
        }
    }
}
```

### Step 6c: Test API Documentation

```bash
# Open in browser
http://your_server_ip:8080/swagger-ui.html

# Or test with curl
curl -s http://your_server_ip:8080/swagger-ui.html | head -20
```

### Step 6d: Verify Database Isolation

```bash
# Try to connect to database from host (should FAIL)
psql -h your_server_ip -U postgres -d notif_db

# Expected result: CONNECTION REFUSED (correct!)
# This confirms PostgreSQL is NOT exposed to host
```

---

## Phase 7: Post-Deployment (5 minutes)

### Step 7a: Create Database Backup

```bash
# Backup database (on server)
docker compose -f docker-compose.prod.yml exec notification-db \
    pg_dump -U postgres notif_db > /opt/notification-service/backup-initial.sql

# Verify backup
ls -lh /opt/notification-service/backup-initial.sql
```

### Step 7b: Set Up Restart Policy

```bash
# Containers already have restart: unless-stopped in compose file
# Verify:
docker compose -f docker-compose.prod.yml ps

# Restart policies:
# - notification-service: automatically restarts on failure
# - notification-db: automatically restarts on failure

# Test restart
docker restart notification-service

# Check status
docker compose -f docker-compose.prod.yml ps

# Should be back UP within 10 seconds
```

### Step 7c: Enable Monitoring (Optional)

```bash
# Add to crontab for health checks
crontab -e

# Add this line (checks every 5 minutes)
*/5 * * * * curl -f http://localhost:8080/actuator/health > /dev/null || \
    docker compose -f /opt/notification-service/docker-compose.prod.yml restart notification-service
```

---

## Quick Reference

### Commands During Deployment

```bash
# View logs (live)
docker compose -f docker-compose.prod.yml logs -f notification-service

# View last N lines
docker compose -f docker-compose.prod.yml logs --tail=100

# Stop services
docker compose -f docker-compose.prod.yml down

# Restart services
docker compose -f docker-compose.prod.yml restart

# Restart one container
docker compose -f docker-compose.prod.yml restart notification-service

# Remove everything (careful!)
docker compose -f docker-compose.prod.yml down -v

# View environment
docker compose -f docker-compose.prod.yml config
```

---

## Troubleshooting

### Problem: Image Pull Fails

```bash
# Verify image name in docker-compose.prod.yml
grep "image:" docker-compose.prod.yml

# Verify image exists on Docker Hub
docker pull your_docker_username/notification-service:1.0.0

# If pull fails, rebuild locally
# (Go back to Phase 1)
```

### Problem: Container Won't Start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs notification-service

# Common issues:
# 1. Database not ready - wait 30 seconds
# 2. Port 8080 in use - kill process or change port
# 3. Env vars wrong - verify .env file
```

### Problem: Database Connection Fails

```bash
# Verify database is running
docker compose -f docker-compose.prod.yml logs notification-db

# Test database connection from app container
docker compose -f docker-compose.prod.yml exec notification-service \
    pg_isready -h notification-db -p 5432

# Check environment variables
grep DB_ .env
```

### Problem: Application Works But No Email Sent

```bash
# Verify email configuration
grep MAIL_ .env

# Check logs for mail errors
docker compose -f docker-compose.prod.yml logs notification-service | grep -i mail

# Test configuration
# Send test notification via API
curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{"recipient":"test@example.com","channel":"EMAIL",...}'
```

---

## Success Criteria ✅

You've successfully deployed when:

- ✅ Both containers show "Up (healthy)"
- ✅ Health endpoint returns `{"status":"UP"}`
- ✅ Swagger UI loads at `/swagger-ui.html`
- ✅ Database is NOT accessible from host
- ✅ No errors in logs
- ✅ Can send test notification via API
- ✅ Email/WhatsApp messages are delivered

---

## Next Steps

1. ✅ Run [Deployment Checklist](CHECKLIST.md) for verification
2. ✅ Set up monitoring (logs, health checks)
3. ✅ Create daily backup schedule
4. ✅ Configure alerts for failures
5. ✅ Document your deployment (write down version, date, credentials location)

---

## Support

- 📖 [Quick Deploy](QUICK_DEPLOY.md) - Fast version of this guide
- 🔧 [Troubleshooting](../reference/TROUBLESHOOTING.md) - Common issues
- 📋 [Checklist](CHECKLIST.md) - Verification steps
- 🐳 [Docker Guide](../docker/OVERVIEW.md) - Docker details

**Questions?** Review the [Troubleshooting Guide](../reference/TROUBLESHOOTING.md) or check logs with:
```bash
docker compose -f docker-compose.prod.yml logs -f
```

---

**🎉 Congratulations on deploying to production!** 🚀
