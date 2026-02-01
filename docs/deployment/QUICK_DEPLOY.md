# Quick Deploy - 5 Minute Setup

**Time: ~5 minutes | Difficulty: Easy**

Get your notification service running in Docker in 5 minutes!

## Prerequisites

- ✅ Docker installed
- ✅ Docker Hub account
- ✅ 10 minutes free

## Step 1: Build & Push (2 minutes)

Open PowerShell in the project root:

```powershell
# Windows
.\build-and-push.ps1 your_docker_username 1.0.0

# Replace "your_docker_username" with your actual Docker Hub username
# Example:
.\build-and-push.ps1 rofiksupriant 1.0.0
```

What happens:
- ✅ Compiles project
- ✅ Builds Docker image (548 MB)
- ✅ Pushes to Docker Hub
- ✅ Done in ~2 minutes

## Step 2: Prepare Server (1 minute)

On your server:

```bash
mkdir -p /opt/notification-service
cd /opt/notification-service
```

From your machine, copy files:

```bash
scp docker/docker-compose.prod.yml user@server:/opt/notification-service/
scp .env.example user@server:/opt/notification-service/.env
```

## Step 3: Configure (1 minute)

SSH to server:

```bash
ssh user@server
cd /opt/notification-service
nano .env
```

Edit these values **only**:

```bash
DOCKER_USERNAME=your_docker_username    # (same as step 1)
APP_VERSION=1.0.0                       # (same as step 1)
DB_PASSWORD=YourStrongPassword123!      # (create new)
NOTIF_API_SECRET=YourAPISecret          # (any random string)
NOTIF_MAIL_USERNAME=your_email@gmail.com
NOTIF_MAIL_PASSWORD=xxxx xxxx xxxx xxxx # (Gmail app password)
```

Save: **Ctrl+X**, **Y**, **Enter**

## Step 4: Deploy (1 minute)

On server:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

Wait ~30 seconds for startup.

## ✅ Verify

```bash
# Check services
docker compose -f docker-compose.prod.yml ps

# Expected output:
# NAME                 STATUS         PORTS
# notification-service Up (healthy)   0.0.0.0:8080->8080/tcp
# notification-db      Up (healthy)   (no exposed ports)

# Test API
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","..."}
```

## 🎉 Done!

Your app is live at:
- **API:** `http://your_server_ip:8080`
- **Swagger:** `http://your_server_ip:8080/swagger-ui.html`
- **Health:** `http://your_server_ip:8080/actuator/health`

---

## 🆘 Issues?

**Docker build fails?**
- Ensure Maven is installed
- Ensure you have ~5GB free disk space
- Try: `mvn clean install -DskipTests`

**Can't connect to server?**
- Verify SSH access: `ssh user@server echo ok`
- Verify ports are open: firewall rules

**App won't start?**
- Check logs: `docker compose -f docker-compose.prod.yml logs notification-service`
- Verify .env file: `cat .env`
- Check database: `docker compose -f docker-compose.prod.yml logs notification-db`

**PostgreSQL connection fails?**
- Verify DB_PASSWORD in .env is correct
- Verify database is running: `docker compose -f docker-compose.prod.yml ps`

---

## 📖 Need More Help?

- [Full Deployment Workflow](WORKFLOW.md) - Detailed explanation of each step
- [Deployment Checklist](CHECKLIST.md) - Complete verification guide
- [Troubleshooting](../reference/TROUBLESHOOTING.md) - Common issues & solutions

---

**Congratulations!** You've successfully deployed the Notification Service! 🚀
