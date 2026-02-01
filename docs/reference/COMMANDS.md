# Docker Commands Reference

Quick copy-paste commands for Docker and Docker Compose.

---

## Docker Hub & Image Management

### Login

```bash
# Login to Docker Hub
docker login

# Enter username and password when prompted
# Credentials stored in ~/.docker/config.json

# Verify login
docker info | grep Username
```

### Build Image

```bash
# Build from Dockerfile in current directory
docker build -t myapp:1.0.0 .

# Build with build args
docker build --build-arg MAVEN_ARGS="-DskipTests" -t myapp:1.0.0 .

# View build progress
docker build -t myapp:1.0.0 . --progress=plain
```

### Tag Image

```bash
# Tag for Docker Hub
docker tag myapp:1.0.0 username/myapp:1.0.0

# Tag as latest
docker tag myapp:1.0.0 username/myapp:latest

# Tag with multiple names
docker tag myapp:1.0.0 username/myapp:1.0.0 username/myapp:latest
```

### Push Image

```bash
# Push single tag
docker push username/myapp:1.0.0

# Push all tags
docker push username/myapp

# Push latest only
docker push username/myapp:latest
```

### Pull Image

```bash
# Pull from Docker Hub
docker pull username/myapp:1.0.0

# Pull latest tag
docker pull username/myapp

# Pull and run
docker run username/myapp:1.0.0
```

### View Images

```bash
# List all images
docker images

# List with size filter
docker images --filter "reference=myapp*"

# Show image details
docker inspect myapp:1.0.0

# View image layers
docker history myapp:1.0.0
```

### Remove Images

```bash
# Remove image
docker rmi myapp:1.0.0

# Remove all unused images
docker image prune -a

# Remove with force
docker rmi -f myapp:1.0.0
```

---

## Container Management

### Run Container

```bash
# Simple run
docker run -d -p 8080:8080 myapp:1.0.0

# With environment variables
docker run -d -p 8080:8080 \
  -e NOTIF_API_SECRET=secret \
  -e DB_PASSWORD=password \
  myapp:1.0.0

# With volume mount
docker run -d -p 8080:8080 \
  -v /host/path:/container/path \
  myapp:1.0.0

# Interactive (bash)
docker run -it myapp:1.0.0 /bin/bash

# Named container
docker run -d --name my-app -p 8080:8080 myapp:1.0.0
```

### View Containers

```bash
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# View container details
docker inspect container_id_or_name

# View resource usage
docker stats
```

### Container Logs

```bash
# View logs
docker logs container_id_or_name

# Live logs (follow)
docker logs -f container_id_or_name

# Last 50 lines
docker logs --tail=50 container_id_or_name

# With timestamps
docker logs -f --timestamps container_id_or_name

# Since specific time
docker logs --since 2024-01-31T10:00:00 container_id_or_name
```

### Execute Commands in Container

```bash
# Run bash
docker exec -it container_id_or_name /bin/bash

# Run specific command
docker exec container_id_or_name curl http://localhost:8080/actuator/health

# As specific user
docker exec -u appuser container_id_or_name whoami
```

### Start/Stop/Restart

```bash
# Start stopped container
docker start container_id_or_name

# Stop running container
docker stop container_id_or_name

# Restart container
docker restart container_id_or_name

# Kill container (force)
docker kill container_id_or_name

# Remove container
docker rm container_id_or_name

# Stop and remove
docker stop container_id_or_name && docker rm container_id_or_name
```

---

## Docker Compose Commands

### Basic Operations

```bash
# Navigate to directory with docker-compose.yml
cd /opt/notification-service

# Start services (foreground)
docker compose up

# Start services (background)
docker compose up -d

# Rebuild and start
docker compose up --build

# Rebuild without starting
docker compose build

# Stop services (keep containers)
docker compose stop

# Stop and remove containers
docker compose down

# Remove everything including volumes (CAREFUL!)
docker compose down -v
```

### Using .env File

```bash
# Start with specific .env file
docker compose --env-file .env up -d

# Production setup
docker compose -f docker-compose.prod.yml --env-file .env up -d

# View what will run (without running)
docker compose -f docker-compose.prod.yml --env-file .env config
```

### Logs

```bash
# View all logs
docker compose logs

# Follow logs (live)
docker compose logs -f

# Last 50 lines
docker compose logs --tail=50

# Specific service
docker compose logs notification-service

# Live logs for one service
docker compose logs -f notification-service

# With timestamps
docker compose logs -f --timestamps
```

### Container Status

```bash
# List containers
docker compose ps

# Detailed status
docker compose ps -a

# View running processes
docker compose top notification-service
```

### Execute Commands

```bash
# Run bash in service
docker compose exec notification-service /bin/bash

# Run specific command
docker compose exec notification-service curl http://localhost:8080/actuator/health

# Execute in different service
docker compose exec notification-db psql -U postgres -d notif_db

# Run as different user
docker compose exec -u postgres notification-db pg_dump notif_db
```

### Restart/Recreate

```bash
# Restart all services
docker compose restart

# Restart specific service
docker compose restart notification-service

# Recreate containers (restart only)
docker compose up -d

# Force recreate (remove and recreate)
docker compose up -d --force-recreate
```

### Database Backup/Restore

```bash
# Backup database
docker compose exec notification-db pg_dump -U postgres notif_db > backup.sql

# Restore database
docker compose exec -T notification-db psql -U postgres notif_db < backup.sql

# Create specific user dump
docker compose exec notification-db pg_dump -U postgres --schema-only notif_db > schema.sql
```

---

## Network & Connectivity

### Network Commands

```bash
# List networks
docker network ls

# Inspect network
docker network inspect app-network

# Create network
docker network create my-network

# Connect container to network
docker network connect my-network container_id

# Disconnect from network
docker network disconnect my-network container_id
```

### Test Connectivity

```bash
# Test from container to host
docker exec container_id ping docker.host.internal

# Test from app container to database
docker compose exec notification-service ping notification-db

# Test database port
docker compose exec notification-service \
  pg_isready -h notification-db -p 5432

# Port scan
docker run --rm -it nicolaka/netshoot nmap -p 5432 notification-db
```

### DNS Resolution

```bash
# Test DNS inside container
docker exec container_id nslookup notification-db

# Resolve service name
docker exec notification-service ping notification-db

# Check /etc/hosts
docker exec container_id cat /etc/hosts
```

---

## Health & Monitoring

### Health Checks

```bash
# View health status
docker inspect --format='{{.State.Health.Status}}' container_id

# View health history
docker inspect container_id | grep -A 10 '"Health"'

# Manual health check
docker exec container_id curl -f http://localhost:8080/actuator/health

# Check service health in compose
docker compose ps
# Look for "(healthy)" or "(unhealthy)" status
```

### Resource Usage

```bash
# View real-time usage
docker stats

# View one container
docker stats container_id

# View specific container in compose
docker compose stats notification-service

# View history
docker stats --no-stream container_id
```

### System Info

```bash
# View Docker system info
docker info

# View disk usage
docker system df

# View images size
docker images --no-trunc

# Detailed image info
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
```

---

## Cleanup & Maintenance

### Remove Unused Resources

```bash
# Remove unused images
docker image prune

# Remove all unused images (including tagged)
docker image prune -a

# Remove unused containers
docker container prune

# Remove unused networks
docker network prune

# Remove unused volumes
docker volume prune

# Remove everything unused
docker system prune -a
```

### Logs Cleanup

```bash
# Clear container logs
docker logs --tail 0 container_id > /dev/null

# Truncate all logs
for container in $(docker ps -aq); do
  docker logs --tail 0 $container > /dev/null 2>&1
done
```

---

## Useful Aliases

Add to `.bashrc` or `.zshrc`:

```bash
# Docker
alias d='docker'
alias di='docker images'
alias dps='docker ps'
alias dl='docker logs -f'

# Docker Compose
alias dc='docker compose'
alias dcup='docker compose up -d'
alias dcdown='docker compose down'
alias dclogs='docker compose logs -f'

# Cleanup
alias dclean='docker system prune -a --force'
alias diclean='docker image prune -a --force'
```

---

## PowerShell (Windows)

### Aliases

```powershell
# Add to PowerShell profile
Set-Alias -Name d -Value docker
Set-Alias -Name dc -Value docker-compose

# Or use functions
function dcup { docker compose up -d }
function dcdown { docker compose down }
function dclogs { docker compose logs -f }
```

### Common Commands

```powershell
# Check Docker status
docker ps

# View logs
docker logs -f container_id

# Run command in container
docker exec container_id powershell Get-ChildItem

# Check image size
docker images --format "{{.Repository}} {{.Size}}"
```

---

## Production Deployment Commands

### Full Deployment Sequence

```bash
# On your machine - Build and push
.\build-and-push.ps1 username 1.0.0

# On server - Prepare
mkdir -p /opt/notification-service
cd /opt/notification-service

# Copy files (from your machine)
scp docker-compose.prod.yml user@server:/opt/notification-service/
scp .env.example user@server:/opt/notification-service/.env

# On server - Configure
ssh user@server
cd /opt/notification-service
nano .env
# Edit values

# On server - Deploy
docker compose -f docker-compose.prod.yml --env-file .env up -d

# Verify
docker compose -f docker-compose.prod.yml ps
curl http://localhost:8080/actuator/health

# Backup
docker compose -f docker-compose.prod.yml exec notification-db \
  pg_dump -U postgres notif_db > backup.sql
```

---

## Troubleshooting Commands

```bash
# Check if Docker daemon is running
docker ps

# View system logs (macOS/Linux)
journalctl -u docker -f

# View system logs (Windows)
Get-EventLog -LogName System -Source Docker | Format-List

# Check disk space
df -h              # Linux/Mac
Get-Volume         # Windows

# Network diagnosis
docker network inspect app-network
docker exec container_id ip addr

# Check ports
netstat -an | grep 8080        # Linux/Mac
Get-NetTCPConnection -LocalPort 8080  # Windows
```

---

## More Information

- 📖 [Docker Compose Spec](https://docs.docker.com/compose/compose-file/)
- 🐳 [Docker CLI Reference](https://docs.docker.com/engine/reference/commandline/docker/)
- 🔒 [Docker Security Best Practices](https://docs.docker.com/engine/security/)
- 📚 [Docker Tutorial](https://docs.docker.com/get-started/)

---

**Tip:** Use `docker --help` and `docker compose --help` for command help.
