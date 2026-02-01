# Docker Compose Configurations

Multiple docker-compose files for different use cases.

---

## 📋 Available Configurations

| File | Database | Build | Use Case |
|------|----------|-------|----------|
| `docker-compose.yml` | ✅ Container | ✅ Local | **Default** - Full local dev |
| `docker-compose.with-db.yml` | ✅ Container | ✅ Local | Full setup with DB container |
| `docker-compose.local-db.yml` | 🏠 Host (local) | ✅ Local | App in Docker, DB on host |
| `docker-compose.app-only.yml` | 🏠 Host (local) | ❌ Hub image | Quick test with published image |
| `docker-compose.prod.yml` | ✅ Container | ❌ Hub image | **Production** deployment |

---

## 1️⃣ Default Development (docker-compose.yml)

**What:** Full local development with database in container  
**Best for:** Most developers, getting started quickly

```bash
# From docker/ directory
docker compose up --build

# Or from project root
docker compose -f docker/docker-compose.yml up --build
```

**Features:**
- ✅ Builds app from local code
- ✅ PostgreSQL in container (port 5432 exposed)
- ✅ Debug logging enabled
- ✅ SQL queries shown in logs
- ✅ Auto-restart on failure
- ✅ Health checks enabled

**Access:**
- App: http://localhost:8080
- Database: localhost:5432 (can use pgAdmin, DBeaver, etc.)

---

## 2️⃣ With Database (docker-compose.with-db.yml)

**What:** Same as default - full setup with DB container  
**Best for:** Teams standardizing on containerized database

```bash
docker compose -f docker-compose.with-db.yml up --build
```

**Identical to `docker-compose.yml`** - use whichever name you prefer.

---

## 3️⃣ Local Database (docker-compose.local-db.yml)

**What:** App in container, PostgreSQL running on host machine  
**Best for:** When you already have PostgreSQL installed locally

```bash
docker compose -f docker-compose.local-db.yml up --build
```

**Prerequisites:**
1. PostgreSQL installed and running on host
2. Database created: `createdb -U postgres notif_db`
3. Accessible from Docker (verify with `psql -U postgres -d notif_db`)

**How it works:**
- App connects to `host.docker.internal:5432`
- This resolves to your host machine's localhost
- Works on Windows, Mac, and Linux (with extra_hosts)

**Features:**
- ✅ Builds app from local code
- ✅ Uses your existing PostgreSQL installation
- ✅ Debug logging enabled
- ✅ No database volume needed

**Access:**
- App: http://localhost:8080
- Database: Already on your host at localhost:5432

---

## 4️⃣ App Only (docker-compose.app-only.yml)

**What:** Pulls pre-built image from Docker Hub, uses local database  
**Best for:** Testing published images quickly

```bash
# Use default image
docker compose -f docker-compose.app-only.yml up -d

# Use specific version
DOCKER_USERNAME=rofiksupriant APP_VERSION=1.0.0 \
  docker compose -f docker-compose.app-only.yml up -d
```

**Prerequisites:**
1. Image pushed to Docker Hub (via `build-and-push.ps1`)
2. PostgreSQL running locally with `notif_db` database

**Features:**
- ❌ No build (pulls from Docker Hub)
- ✅ Uses local PostgreSQL
- ✅ Fast startup (no build time)
- ✅ Test production image locally

**Environment Variables:**
- `DOCKER_USERNAME` - Your Docker Hub username (default: rofiksupriant)
- `APP_VERSION` - Image tag/version (default: latest)

---

## 5️⃣ Production (docker-compose.prod.yml)

**What:** Production deployment with isolated database  
**Best for:** Deploying to servers

```bash
# On server with .env file
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

**Features:**
- ❌ No build (pulls from Docker Hub)
- ✅ PostgreSQL in container (NOT exposed to host)
- ✅ Environment-based secrets (from `.env`)
- ✅ Health checks with auto-restart
- ✅ Production logging (INFO level)
- ✅ Security hardened

**See:** [docs/deployment/WORKFLOW.md](../docs/deployment/WORKFLOW.md)

---

## 🚀 Quick Start Guide

### First Time Setup (Recommended)

```bash
# Use default - full setup with DB
cd docker
docker compose up --build

# Wait for startup (30-60 seconds)
# Access: http://localhost:8080
```

### Already Have PostgreSQL?

```bash
# Use local database version
cd docker
docker compose -f docker-compose.local-db.yml up --build

# Access: http://localhost:8080
```

### Testing Published Image?

```bash
# Use app-only (no build)
cd docker
docker compose -f docker-compose.app-only.yml up -d

# Access: http://localhost:8080
```

---

## 📊 Comparison

### Database Location

| File | Database Location | Port Exposed? | Can Access DB? |
|------|------------------|---------------|----------------|
| `docker-compose.yml` | Container | ✅ Yes (5432) | ✅ Yes |
| `docker-compose.with-db.yml` | Container | ✅ Yes (5432) | ✅ Yes |
| `docker-compose.local-db.yml` | Host machine | N/A | ✅ Yes (native) |
| `docker-compose.app-only.yml` | Host machine | N/A | ✅ Yes (native) |
| `docker-compose.prod.yml` | Container | ❌ No (internal) | ❌ No (secure) |

### Build vs Pull

| File | Source | Build Time | Internet Required |
|------|--------|------------|-------------------|
| `docker-compose.yml` | Local code | ~2 min | No |
| `docker-compose.with-db.yml` | Local code | ~2 min | No |
| `docker-compose.local-db.yml` | Local code | ~2 min | No |
| `docker-compose.app-only.yml` | Docker Hub | ~10 sec | ✅ Yes |
| `docker-compose.prod.yml` | Docker Hub | ~10 sec | ✅ Yes |

---

## 🛠️ Common Commands

### Start Services

```bash
# Default (with DB)
docker compose up -d

# Specific file
docker compose -f docker-compose.local-db.yml up -d

# With build
docker compose up --build
```

### Stop Services

```bash
# Stop (keeps containers)
docker compose down

# Stop and remove volumes (deletes data!)
docker compose down -v
```

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f notification-service
```

### Restart

```bash
# Restart all
docker compose restart

# Restart app only
docker compose restart notification-service
```

### Database Access

**When using containerized database:**
```bash
# Connect via docker exec
docker compose exec notification-db psql -U postgres -d notif_db

# Or from host (when port 5432 is exposed)
psql -h localhost -U postgres -d notif_db
```

**When using local database:**
```bash
# Connect normally
psql -U postgres -d notif_db
```

---

## 🔧 Troubleshooting

### "Connection refused" to Database

**With containerized DB:**
```bash
# Check if DB is running
docker compose ps

# Check DB health
docker compose logs notification-db

# Wait for health check (can take 30 seconds)
# Look for "database system is ready to accept connections"
```

**With local DB:**
```bash
# Verify PostgreSQL is running
psql -U postgres -c "SELECT 1;"

# Verify database exists
psql -U postgres -l | grep notif_db

# Create database if missing
createdb -U postgres notif_db
```

### Port Already in Use

```bash
# Find what's using port 8080
lsof -i :8080  # Linux/Mac
Get-NetTCPConnection -LocalPort 8080  # Windows

# Kill process or change port in compose file
# ports: - "9090:8080"  # Use 9090 instead
```

### Image Not Found (app-only)

```bash
# Verify image exists on Docker Hub
docker pull rofiksupriant/notification-service:latest

# If missing, build and push first
cd ..
./build-and-push.ps1 your_username 1.0.0
```

### Can't Connect to host.docker.internal (Linux)

```bash
# Use network host mode instead
docker compose -f docker-compose.local-db.yml up --network host

# Or update NOTIF_DB_URL to use 172.17.0.1 (Docker bridge IP)
```

---

## 📖 More Information

- **Setup Guide:** [docs/deployment/SETUP.md](../docs/deployment/SETUP.md)
- **Deployment Guide:** [docs/deployment/WORKFLOW.md](../docs/deployment/WORKFLOW.md)
- **Docker Guide:** [docs/docker/OVERVIEW.md](../docs/docker/OVERVIEW.md)
- **Commands Reference:** [docs/reference/COMMANDS.md](../docs/reference/COMMANDS.md)
- **Troubleshooting:** [docs/reference/TROUBLESHOOTING.md](../docs/reference/TROUBLESHOOTING.md)

---

## 🎯 Which One Should I Use?

| Your Situation | Use This |
|----------------|----------|
| **First time setup** | `docker-compose.yml` (default) |
| **I have PostgreSQL installed** | `docker-compose.local-db.yml` |
| **Testing a published image** | `docker-compose.app-only.yml` |
| **Deploying to server** | `docker-compose.prod.yml` |
| **Team standardization** | `docker-compose.with-db.yml` |

**Most common:** `docker-compose.yml` or `docker-compose.local-db.yml`

---

**Last Updated:** January 31, 2026  
**Status:** ✅ All configurations tested and working
