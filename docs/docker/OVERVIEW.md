# Docker Overview

Understanding Docker for this project.

---

## What is Docker?

Docker is a containerization technology that packages your application with all dependencies into a portable unit.

### The Problem Docker Solves

```
Without Docker:
┌─────────────────────────────────┐
│         Your Laptop             │
│  Java 21, PostgreSQL, RabbitMQ  │
├─────────────────────────────────┤
│     Notification Service        │
│         (Works!)                │
└─────────────────────────────────┘
                ↓
        "Works on my machine"
                ↓
┌─────────────────────────────────┐
│        Production Server        │
│  Java 17, PostgreSQL 14 only    │
├─────────────────────────────────┤
│     Notification Service        │
│     (Broken - wrong versions!)  │
└─────────────────────────────────┘
```

```
With Docker:
┌──────────────────────────────────┐
│    Docker Container (Image)      │
│  ┌────────────────────────────┐  │
│  │  Java 21 (built-in)        │  │
│  │  Spring Boot 3.5+          │  │
│  │  All dependencies          │  │
│  │  Your application          │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
        Same on laptop, server, cloud
        Works everywhere!
```

---

## Docker Concepts

### 1. Image

A blueprint - like a recipe or template.

```
Dockerfile:
┌─────────────────────┐
│ FROM java:21        │
│ COPY app.jar /      │
│ ENTRYPOINT [java]   │
└─────────────────────┘
        ↓
    Build image
        ↓
┌─────────────────────┐
│  Docker Image       │
│  (548 MB file)      │
│  Can be stored,     │
│  shared, versioned  │
└─────────────────────┘
```

### 2. Container

A running instance of an image - like running an executable.

```
Docker Image
    ↓
docker run
    ↓
┌──────────────────┐
│   Container      │
│  (Running app)   │
│  Process ID 1234 │
│  Port 8080       │
└──────────────────┘
```

### 3. Docker Compose

Orchestrates multiple containers together.

```
docker-compose.yml:
┌──────────────────────────────────┐
│  notification-service:           │
│    image: notification:1.0.0     │
│    ports: 8080:8080              │
│    depends_on: postgres          │
│                                  │
│  postgres:                       │
│    image: postgres:17-alpine     │
│    no exposed ports (internal)   │
│    environment: DB_PASSWORD      │
└──────────────────────────────────┘
        ↓
    docker compose up
        ↓
Both containers start & connected
```

---

## Our Docker Setup

### Dockerfile

Located: `./Dockerfile`

```dockerfile
# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn clean install -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
# Copy built app
COPY --from=builder /build/target/notification-service.jar /app.jar
# Create non-root user
RUN useradd -m appuser
USER appuser
# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health
# Start app
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**What it does:**
1. Stage 1: Compiles your code with Maven
2. Stage 2: Creates runtime image with only JRE (not JDK)
3. Copies compiled app
4. Creates non-root user (security)
5. Adds health check
6. Final size: ~548 MB

### docker-compose.prod.yml

Located: `./docker/docker-compose.prod.yml`

```yaml
services:
  notification-service:
    image: ${DOCKER_USERNAME}/notification-service:${APP_VERSION}
    ports:
      - "8080:8080"        # Exposed: Anyone can access
    environment:
      NOTIF_DB_PASSWORD: ${DB_PASSWORD}
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:17-alpine
    # NO ports defined
    # PostgreSQL is INTERNAL ONLY
    # Not accessible from host
    # Only app container can access
```

**Key Features:**
- 🔒 PostgreSQL NOT exposed to host
- 🌐 Only app port 8080 exposed
- 🏥 Health checks enabled
- ↗️ Auto-restart on failure
- 🎯 Internal network for communication

---

## Docker Workflow

### Development (Local)

```
┌──────────────────┐
│ docker-compose   │
│ (dev setup)      │
└──────────────────┘
        ↓
┌──────────────────────────────────┐
│  Local containers:               │
│  • notification-service:8080     │
│  • postgres (exposed 5432)       │
└──────────────────────────────────┘
        ↓
Test locally
```

**File:** `docker/docker-compose.yml` (dev)

### Production

```
┌─────────────────┐
│  build-and-     │
│  push.ps1       │
└─────────────────┘
        ↓
┌──────────────────────────────┐
│  1. mvn clean install        │
│  2. docker build (image)     │
│  3. docker push (Docker Hub) │
└──────────────────────────────┘
        ↓
┌──────────────────────────────┐
│   Docker Hub Registry        │
│  your_user/notification...   │
│  :1.0.0 tag                  │
│  :latest tag                 │
└──────────────────────────────┘
        ↓
┌────────────────────────────────────┐
│  Server (docker-compose.prod.yml)  │
│  docker pull from Docker Hub       │
│  Start containers                  │
│  postgres: internal only           │
│  app: port 8080 exposed            │
└────────────────────────────────────┘
```

**File:** `docker/docker-compose.prod.yml` (prod)

---

## Key Commands

### Build Docker Image

```bash
# From project root
docker build -t myapp:1.0.0 .

# What happens:
# 1. Reads Dockerfile
# 2. Executes each line
# 3. Creates image layers
# 4. Final image: myapp:1.0.0
```

### Run Container

```bash
# Simple run
docker run -p 8080:8080 myapp:1.0.0

# With environment variables
docker run -p 8080:8080 \
  -e DB_PASSWORD=secret \
  -e NOTIF_API_SECRET=key \
  myapp:1.0.0

# Detached (background)
docker run -d -p 8080:8080 myapp:1.0.0
```

### Docker Compose

```bash
# Start services
docker compose up              # Foreground
docker compose up -d           # Background

# View logs
docker compose logs
docker compose logs -f         # Live feed

# Stop services
docker compose down            # Stop & remove containers
docker compose stop            # Just stop (keep containers)

# Restart
docker compose restart
```

### Push to Docker Hub

```bash
# Login first
docker login

# Tag image
docker tag myapp:1.0.0 username/myapp:1.0.0

# Push
docker push username/myapp:1.0.0

# Pull on another machine
docker pull username/myapp:1.0.0
docker run -p 8080:8080 username/myapp:1.0.0
```

---

## File Structure

```
notification-service/
├── Dockerfile                           (Image definition)
├── .dockerignore                        (Files to exclude)
├── build-and-push.ps1                   (Build & push script)
│
├── docker/
│   ├── docker-compose.yml               (Dev - exposed DB)
│   ├── docker-compose.prod.yml          (Prod - internal DB)
│   └── .env.example                     (Config template)
│
└── src/
    ├── main/java/...                    (Your code)
    ├── main/resources/
    │   ├── application.yml              (Config)
    │   └── db/migration/                (Database migrations)
    └── ...
```

---

## Environment Variables

Variables passed to running container:

```bash
# In docker-compose.prod.yml
environment:
  DOCKER_USERNAME: ${DOCKER_USERNAME}    # From .env
  APP_VERSION: ${APP_VERSION}            # From .env
  DB_PASSWORD: ${DB_PASSWORD}            # From .env
  NOTIF_API_SECRET: ${NOTIF_API_SECRET}  # From .env
  ...
```

```bash
# .env file (server)
DOCKER_USERNAME=rofiksupriant
APP_VERSION=1.0.0
DB_PASSWORD=SecurePassword123!
NOTIF_API_SECRET=RandomSecret
...
```

The container reads these and uses them in the application.

---

## Network & Security

### Container Communication

```
Host Machine (Server)
┌───────────────────────────────────┐
│                                   │
│  Port 8080 EXPOSED                │
│  ┌─────────────────────────────┐  │
│  │  notification-service       │  │
│  │  Listens: 0.0.0.0:8080      │  │
│  │  Access: http://server:8080 │  │
│  └──────────────┬──────────────┘  │
│                 │                  │
│  app-network    │                  │
│  (bridge)       │                  │
│  Internal only  │                  │
│                 │                  │
│  ┌──────────────▼──────────────┐  │
│  │    notification-db          │  │
│  │    PostgreSQL 5432          │  │
│  │    NO exposed port          │  │
│  │    Only accessible from app │  │
│  └─────────────────────────────┘  │
│                                   │
└───────────────────────────────────┘

Access from host: ✓ Can access app
Access from host: ✗ Cannot access database
Database only accessible from app
```

### What's Exposed?

```bash
# EXPOSED (public internet can access)
Port 8080 (HTTP)
→ Notification Service API
→ Swagger UI
→ Health checks

# NOT EXPOSED (internal only)
Port 5432 (PostgreSQL)
→ Only from app container
→ Not from host/internet
→ Secure!
```

---

## Security Features

### 1. Non-Root User

```dockerfile
RUN useradd -m appuser
USER appuser
```

Container runs as `appuser`, not `root`. If container is compromised, attacker has limited privileges.

### 2. Network Isolation

```yaml
services:
  postgres:
    # NO ports: exposed
    # Only accessible via network name: notification-db
    # Not exposed to host or internet
```

Database is only accessible from within the Docker network.

### 3. Environment Variables

```bash
# Secrets in .env (not committed to git)
DB_PASSWORD=secret123

# .env in .gitignore
# Applied at runtime
# Not baked into image
```

### 4. Health Checks

```dockerfile
HEALTHCHECK --interval=30s \
  CMD curl -f http://localhost:8080/actuator/health
```

Automatic restart if unhealthy.

### 5. Read-Only File System (Optional)

```yaml
read_only: true
tmpfs:
  - /tmp
  - /var/run
```

Can make filesystem read-only for extra security.

---

## Common Use Cases

### Local Development

```bash
cd docker
docker-compose up --build

# Access: http://localhost:8080
# PostgreSQL: localhost:5432 (exposed for local testing)
```

### Testing

```bash
# Build image
docker build -t test:latest .

# Run tests
docker run test:latest mvn test

# Or run container and test manually
docker run -p 8080:8080 test:latest
# Test at http://localhost:8080
```

### Production Deployment

```bash
# 1. Build & push
./build-and-push.ps1 username 1.0.0

# 2. On server
cd /opt/notification-service
docker compose -f docker-compose.prod.yml --env-file .env up -d

# 3. Access
http://server_ip:8080
```

---

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker compose logs notification-service

# Restart
docker compose restart notification-service

# Check status
docker compose ps
```

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill it or change port in compose file
docker-compose.prod.yml: ports: - "9090:8080"
```

### Image Too Large

```bash
# Remove intermediate images
docker image prune -a

# Check image size
docker images | grep notification
```

---

## Next Steps

- 📖 [Build Docker Image](BUILD.md) - Building process
- 🔒 [Container Security](SECURITY.md) - Security details
- 🎵 [Docker Compose](COMPOSE.md) - Orchestration guide
- ⚡ [Commands Reference](../reference/COMMANDS.md) - Command list
- 🚀 [Deployment](../deployment/WORKFLOW.md) - Deploy to production

---

**Questions?** See [Docker Commands](../reference/COMMANDS.md) for quick reference.
