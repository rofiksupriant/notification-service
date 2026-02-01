# Deployment Checklist

Complete verification before and after deployment.

---

## ✅ Pre-Deployment Checklist

Run these checks **before** starting deployment.

### Code & Build

- [ ] All code committed and pushed to git
- [ ] Latest code pulled on your machine
- [ ] No uncommitted changes: `git status`
- [ ] All tests passing: `mvn test`
- [ ] No build warnings: `mvn clean install -DskipTests`
- [ ] Docker installed and running: `docker --version`
- [ ] Maven installed: `mvn --version`
- [ ] Java 21+ installed: `java -version`

### Configuration

- [ ] `.env.example` exists and is up to date
- [ ] No secrets in `.env.example` (only placeholders)
- [ ] `.env` file is in `.gitignore`
- [ ] Docker Hub account created and credentials ready
- [ ] Verified Docker Hub login: `docker login`

### Server Preparation

- [ ] SSH access verified: `ssh user@server echo ok`
- [ ] Server has Docker installed: `ssh user@server docker --version`
- [ ] Server has Docker Compose: `ssh user@server docker compose --version`
- [ ] Server has 5GB+ free disk space
- [ ] Server network allows outbound HTTPS (for image pull)
- [ ] Port 8080 is available on server (or port you chose)

### Database & Services

- [ ] Email account prepared (Gmail with app password)
- [ ] WhatsApp (Watzap) API key obtained
- [ ] Database password generated (16+ characters)
- [ ] API secret generated: `openssl rand -base64 32`
- [ ] RabbitMQ not required (disabled in config)

---

## ✅ Deployment Checklist

Run these checks **during** deployment.

### Build Phase

- [ ] PowerShell script ready: `build-and-push.ps1` exists
- [ ] Docker Hub credentials active: `docker login`
- [ ] Username selected for Docker Hub
- [ ] Version number chosen (e.g., 1.0.0)
- [ ] Ran: `.\build-and-push.ps1 username version`
- [ ] Build completed successfully
- [ ] Image pushed to Docker Hub
- [ ] Verified on Docker Hub: image visible with tags

### Server Preparation

- [ ] Created directory: `/opt/notification-service`
- [ ] Copied files:
  - [ ] `docker-compose.prod.yml`
  - [ ] `.env.example` (as `.env`)
- [ ] Files verified on server: `ls -la /opt/notification-service/`
- [ ] Directory permissions correct: `ls -ld /opt/notification-service/`
- [ ] `.env` file permissions: `chmod 600 .env` (read-only)

### Configuration

- [ ] Edited `.env` with actual values
- [ ] `DOCKER_USERNAME` matches Docker Hub username
- [ ] `APP_VERSION` matches pushed version (e.g., 1.0.0)
- [ ] `DB_PASSWORD` is strong (16+ characters, mixed case/numbers)
- [ ] `NOTIF_API_SECRET` is set to random value
- [ ] `NOTIF_MAIL_USERNAME` is valid email
- [ ] `NOTIF_MAIL_PASSWORD` is Gmail app password (NOT regular password)
- [ ] `NOTIF_WATZAP_API_KEY` is valid
- [ ] `NOTIF_WATZAP_PHONE_ID` is valid
- [ ] No real passwords in `.env.example` (only in actual `.env`)

### Deployment

- [ ] Ran: `docker compose -f docker-compose.prod.yml --env-file .env up -d`
- [ ] Output shows pulling image
- [ ] Output shows creating containers
- [ ] No error messages
- [ ] Waited 30+ seconds for startup

---

## ✅ Post-Deployment Checklist

Run these checks **after** deployment.

### Container Status

- [ ] Both containers running: `docker compose -f docker-compose.prod.yml ps`
- [ ] notification-service shows "Up (healthy)"
- [ ] notification-db shows "Up (healthy)"
- [ ] Container ports correctly mapped: `8080:8080` (app), no port (db)
- [ ] No containers restarting: `docker compose -f docker-compose.prod.yml ps`

### Health Checks

- [ ] Health endpoint responds: `curl http://localhost:8080/actuator/health`
- [ ] Response shows `"status":"UP"`
- [ ] Database health shows "UP"
- [ ] All components show "UP"

### Application Access

- [ ] Swagger UI loads: `http://server_ip:8080/swagger-ui.html`
- [ ] API responds to requests
- [ ] No authentication errors
- [ ] No database connection errors

### Database Security

- [ ] PostgreSQL NOT accessible from host: `psql -h server_ip -U postgres` (should fail)
- [ ] Database ONLY accessible from app container
- [ ] No exposed ports in `docker-compose.prod.yml` for database
- [ ] Database on internal network (verified in compose file)

### Network Security

- [ ] Only port 8080 exposed to host/internet
- [ ] PostgreSQL port 5432 NOT exposed to host
- [ ] Containers on internal bridge network
- [ ] No incoming traffic on port 5432

### Logs & Monitoring

- [ ] No error messages in logs: `docker compose -f docker-compose.prod.yml logs`
- [ ] Application started successfully (look for "Started NotificationServiceApplication")
- [ ] Database migrations completed (look for "Flyway" or migration messages)
- [ ] No repeated error messages indicating crash loops

### Functional Testing

- [ ] Can access API documentation
- [ ] Health check returns healthy status
- [ ] Can view Swagger endpoints
- [ ] No SQL errors in database communication
- [ ] Application logs show normal startup

### Data Persistence

- [ ] Created database backup: `docker compose exec notification-db pg_dump -U postgres notif_db > backup.sql`
- [ ] Backup file exists and is not empty
- [ ] Can query database from app container

### Restart Policy

- [ ] Containers have restart policy: `unless-stopped`
- [ ] Verified in `docker-compose.prod.yml`
- [ ] If stopped, will restart automatically

---

## ✅ Verification Tests

Perform these tests to verify functionality.

### API Test

```bash
# Test sending notification
curl -X POST http://server_ip:8080/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "slug": "test-template",
    "recipient": "test@example.com",
    "channel": "EMAIL",
    "language": "en",
    "variables": {"userName": "Test User"}
  }'

# Expected: 202 Accepted
# Notification queued for processing
```

### Health Check Test

```bash
# Direct health check
curl -s http://localhost:8080/actuator/health | grep status

# Expected output:
# "status":"UP"
```

### Database Test

```bash
# Test database connectivity
docker compose -f docker-compose.prod.yml exec notification-service \
  pg_isready -h notification-db -p 5432

# Expected: accepting connections
```

### Logging Test

```bash
# View recent logs (look for errors)
docker compose -f docker-compose.prod.yml logs --tail=20 notification-service

# Should show:
# - Application started successfully
# - No error messages
# - Database migrations completed
```

---

## ✅ Post-Deployment Maintenance

Complete these after successful deployment.

### Documentation

- [ ] Recorded deployment date and version in wiki/docs
- [ ] Saved `.env` location (back it up separately, secure location)
- [ ] Documented database credentials (secure location only)
- [ ] Updated deployment notes with any custom configurations

### Backups

- [ ] Database backup taken: `backup-initial.sql`
- [ ] Backup stored in secure location (NOT in git)
- [ ] Backup verified (test restore if possible)
- [ ] Set up scheduled backups (daily)

### Monitoring

- [ ] Set up log rotation (if not automatic)
- [ ] Enable health check monitoring
- [ ] Set up alerts for unhealthy containers
- [ ] Monitor disk space on server

### Security

- [ ] `.env` file backed up (secure location)
- [ ] `.env` file permissions verified: `600`
- [ ] No `.env` file committed to git
- [ ] Database password in secure vault (not in git)
- [ ] Credentials rotated (document in secure location)

---

## 🆘 Troubleshooting Quick Links

If something fails, check:

| Issue | Solution |
|-------|----------|
| Containers won't start | Check logs: `docker compose logs` |
| Can't pull image | Verify Docker Hub login: `docker login` |
| Port 8080 in use | Change in docker-compose.prod.yml or kill process |
| Database won't connect | Check DB_PASSWORD in .env matches |
| Email not sending | Verify email config and Gmail app password |
| Health check failing | Wait 30+ seconds, check logs for errors |
| Container keeps restarting | Review logs for startup errors |
| Database not migrating | Verify database password, check flyway logs |

**Full troubleshooting:** See [Troubleshooting Guide](../reference/TROUBLESHOOTING.md)

---

## 📋 Sign-Off

After all checks pass:

```
Deployment Date: ______________
Version: ______________
Deployed By: ______________
Server IP: ______________

All checks passed: ☐

Signature: ______________
```

---

## 📞 Support

- 📖 [Full Workflow](WORKFLOW.md) - Detailed deployment steps
- 🔧 [Troubleshooting](../reference/TROUBLESHOOTING.md) - Common issues
- 🐳 [Docker Guide](../docker/OVERVIEW.md) - Docker questions
- ⚡ [Quick Deploy](QUICK_DEPLOY.md) - Fast deployment

**✅ If all checkboxes are checked, deployment is successful!**
