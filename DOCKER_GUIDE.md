# Docker Guide for Kabengo Safaris

A comprehensive guide for using Docker in development and production environments.

---

## Table of Contents

1. [Introduction to Docker](#1-introduction-to-docker)
2. [Docker Architecture](#2-docker-architecture)
3. [Installation](#3-installation)
4. [Docker Compose Files Overview](#4-docker-compose-files-overview)
5. [Development Environment](#5-development-environment)
6. [Production Environment](#6-production-environment)
7. [Common Docker Commands](#7-common-docker-commands)
8. [Docker Compose Commands](#8-docker-compose-commands)
9. [Volume Management](#9-volume-management)
10. [Networking](#10-networking)
11. [Health Checks](#11-health-checks)
12. [Resource Management](#12-resource-management)
13. [Troubleshooting](#13-troubleshooting)
14. [Best Practices](#14-best-practices)
15. [Quick Reference Cheat Sheet](#15-quick-reference-cheat-sheet)

---

## 1. Introduction to Docker

### What is Docker?

Docker is a platform that packages applications and their dependencies into **containers** - lightweight, portable, self-sufficient units that can run anywhere Docker is installed.

### Key Concepts

| Concept | Description |
|---------|-------------|
| **Image** | A read-only template containing the application and its dependencies. Like a "snapshot" of an application. |
| **Container** | A running instance of an image. You can have multiple containers from one image. |
| **Volume** | Persistent storage that survives container restarts/deletions. |
| **Network** | Virtual networks that allow containers to communicate. |
| **Docker Compose** | Tool for defining and running multi-container applications using YAML files. |

### Why Use Docker?

- **Consistency**: Same environment in development, testing, and production
- **Isolation**: Each service runs in its own container
- **Portability**: Works on any machine with Docker installed
- **Scalability**: Easy to scale services up or down
- **Version Control**: Images can be versioned and rolled back

---

## 2. Docker Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Your Machine (Host)                     │
├─────────────────────────────────────────────────────────────┤
│                      Docker Engine                          │
├───────────────┬───────────────┬───────────────┬─────────────┤
│  Container 1  │  Container 2  │  Container 3  │     ...     │
│  (MySQL)      │  (LibreTrans) │  (Redis)      │             │
├───────────────┴───────────────┴───────────────┴─────────────┤
│                    Docker Network                           │
├─────────────────────────────────────────────────────────────┤
│                    Docker Volumes                           │
│            (Persistent data storage)                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Installation

### Kali Linux / Debian / Ubuntu

```bash
# Install Docker Engine
sudo apt update
sudo apt install docker.io

# Install Docker Compose
sudo apt install docker-compose

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker  # Start on boot

# Add your user to docker group (to run without sudo)
sudo usermod -aG docker $USER
# Log out and log back in for this to take effect
```

### Verify Installation

```bash
# Check Docker version
docker --version

# Check Docker Compose version
docker-compose --version

# Test Docker works
docker run hello-world
```

---

## 4. Docker Compose Files Overview

This project has two Docker Compose files:

### File Comparison

| Feature | `docker-compose.yml` | `docker-compose-production.yml` |
|---------|---------------------|---------------------------|
| **Purpose** | Development | Production |
| **Image Version** | `v1.6.1` (stable) | `latest` |
| **Languages** | en, fr, de, es, it (5) | en, fr, de, es, it, pt, sw (7) |
| **Volume Persistence** | No | Yes |
| **Resource Limits** | No | Yes (4GB limit, 2GB reserved) |
| **Auto-update Models** | Yes | Yes |
| **Health Check Start Period** | 180s | 60s |

### docker-compose.yml (Development)

```yaml
services:
  libretranslate:
    image: libretranslate/libretranslate:v1.6.1  # Stable version
    container_name: kabengosafaris-libretranslate
    restart: unless-stopped
    ports:
      - "5000:5000"
    environment:
      - LT_LOAD_ONLY=en,fr,de,es,it     # Fewer languages = faster startup
      - LT_SUGGESTIONS=false
      - LT_CHAR_LIMIT=10000
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/languages"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 180s                 # Allow time for model download
```

**When to use:** Local development, testing, quick iterations.

### docker-compose-production.yml (Production)

```yaml
services:
  libretranslate:
    image: libretranslate/libretranslate:latest  # Latest features
    container_name: kabengosafaris-libretranslate
    restart: unless-stopped
    ports:
      - "5000:5000"
    environment:
      - LT_LOAD_ONLY=en,fr,de,es,it,pt,sw  # All needed languages
      - LT_DISABLE_WEB_UI=false
      - LT_UPDATE_MODELS=true               # Auto-update models
      - LT_SUGGESTIONS=false
      - LT_CHAR_LIMIT=10000
      - LT_REQ_LIMIT=0
      - LT_THREADS=4
    volumes:
      - libretranslate_data:/home/libretranslate/.local/share/argos-translate
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/languages"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 2G

volumes:
  libretranslate_data:
    driver: local
```

**When to use:** Production servers, staging environments.

---

## 5. Development Environment

### Starting Development Environment

```bash
# Navigate to project directory
cd "/home/ricksy/Documents/SPRING BOOT PROJECTS/kabengosafaris"

# Start containers (uses docker-compose.yml by default)
sudo docker-compose up -d

# View logs
sudo docker-compose logs -f libretranslate

# Stop containers
sudo docker-compose down
```

### Development Workflow

```
┌──────────────────────────────────────────────────────────────┐
│                    Development Workflow                      │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│   1. Start Docker containers                                 │
│      └── sudo docker-compose up -d                           │
│                                                              │
│   2. Start Spring Boot application                           │
│      └── mvn spring-boot:run                                 │
│                                                              │
│   3. Develop & Test                                          │
│      └── Application connects to LibreTranslate at :5000     │
│                                                              │
│   4. Stop when done                                          │
│      └── sudo docker-compose down                            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Development Tips

1. **Don't use volumes in dev**: Faster restarts, fresh state each time
2. **Use stable image versions**: Avoid unexpected breaking changes
3. **Fewer languages**: Faster startup time for quicker iterations
4. **No resource limits**: Use all available resources for speed

---

## 6. Production Environment

### Starting Production Environment

```bash
# Use the full configuration file
sudo docker-compose -f docker-compose-production.yml up -d

# View status
sudo docker-compose -f docker-compose-production.yml ps

# View logs
sudo docker-compose -f docker-compose-production.yml logs -f

# Stop
sudo docker-compose -f docker-compose-production.yml down
```

### Production Deployment Checklist

- [ ] Use `docker-compose-production.yml`
- [ ] Verify volume is configured for data persistence
- [ ] Set appropriate resource limits for your server
- [ ] Configure firewall to restrict port 5000 (only allow from app server)
- [ ] Set up monitoring and alerts
- [ ] Configure log rotation
- [ ] Enable `restart: unless-stopped` (already set)
- [ ] Test health checks are working

### Production Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Production Server                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────────┐      ┌──────────────────────────┐     │
│   │  Spring Boot App    │      │  LibreTranslate          │     │
│   │  (Port 8080)        │─────▶│  (Port 5000)             │     │
│   │                     │      │                          │     │
│   │  - PDF Generation   │      │  - Translation API       │     │
│   │  - Business Logic   │      │  - Language Models       │     │
│   └─────────────────────┘      └──────────────────────────┘     │
│            │                              │                     │
│            ▼                              ▼                     │
│   ┌─────────────────────┐      ┌──────────────────────────┐     │
│   │  MySQL Database     │      │  Docker Volume           │     │
│   │  (Port 3306)        │      │  (libretranslate_data)   │     │
│   └─────────────────────┘      └──────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Security Considerations

```bash
# Restrict LibreTranslate to only accept connections from localhost
# Edit docker-compose-production.yml:
ports:
  - "127.0.0.1:5000:5000"  # Only accessible from same machine

# Or use Docker networks for internal communication only
# (Don't expose port externally)
```

---

## 7. Common Docker Commands

### Container Management

| Command | Description |
|---------|-------------|
| `docker ps` | List running containers |
| `docker ps -a` | List all containers (including stopped) |
| `docker start <name>` | Start a stopped container |
| `docker stop <name>` | Stop a running container |
| `docker restart <name>` | Restart a container |
| `docker rm <name>` | Remove a stopped container |
| `docker rm -f <name>` | Force remove (even if running) |

### Image Management

| Command | Description |
|---------|-------------|
| `docker images` | List downloaded images |
| `docker pull <image>` | Download an image |
| `docker rmi <image>` | Remove an image |
| `docker image prune` | Remove unused images |

### Logs & Debugging

| Command | Description |
|---------|-------------|
| `docker logs <name>` | View container logs |
| `docker logs -f <name>` | Follow logs (live) |
| `docker logs --tail 100 <name>` | Last 100 log lines |
| `docker exec -it <name> /bin/sh` | Shell into container |
| `docker inspect <name>` | View container details |

### Examples

```bash
# View LibreTranslate container logs
sudo docker logs kabengosafaris-libretranslate

# Follow logs in real-time
sudo docker logs -f kabengosafaris-libretranslate

# Execute command inside container
sudo docker exec -it kabengosafaris-libretranslate ls -la

# Get shell access inside container
sudo docker exec -it kabengosafaris-libretranslate /bin/sh

# Inspect container configuration
sudo docker inspect kabengosafaris-libretranslate
```

---

## 8. Docker Compose Commands

### Basic Commands

| Command | Description |
|---------|-------------|
| `docker-compose up` | Start all services (foreground) |
| `docker-compose up -d` | Start all services (background/detached) |
| `docker-compose down` | Stop and remove containers |
| `docker-compose down -v` | Stop, remove containers AND volumes |
| `docker-compose restart` | Restart all services |
| `docker-compose ps` | List running services |
| `docker-compose logs` | View logs for all services |

### Using Different Compose Files

```bash
# Default (uses docker-compose.yml)
sudo docker-compose up -d

# Specify a different file
sudo docker-compose -f docker-compose-production.yml up -d

# Use multiple files (merged)
sudo docker-compose -f docker-compose.yml -f docker-compose.override.yml up -d
```

### Service-Specific Commands

```bash
# Start only LibreTranslate service
sudo docker-compose up -d libretranslate

# View logs for specific service
sudo docker-compose logs -f libretranslate

# Restart specific service
sudo docker-compose restart libretranslate

# Stop specific service
sudo docker-compose stop libretranslate
```

### Rebuild & Update

```bash
# Pull latest images
sudo docker-compose pull

# Rebuild and restart (after image update)
sudo docker-compose up -d --build

# Force recreate containers
sudo docker-compose up -d --force-recreate
```

---

## 9. Volume Management

### What are Volumes?

Volumes persist data outside the container lifecycle. Without volumes, all data is lost when a container is removed.

### Volume Commands

| Command | Description |
|---------|-------------|
| `docker volume ls` | List all volumes |
| `docker volume inspect <name>` | View volume details |
| `docker volume rm <name>` | Remove a volume |
| `docker volume prune` | Remove unused volumes |

### Examples

```bash
# List all volumes
sudo docker volume ls

# Inspect the LibreTranslate data volume
sudo docker volume inspect kabengosafaris_libretranslate_data

# See where volume data is stored on host
sudo docker volume inspect kabengosafaris_libretranslate_data --format '{{ .Mountpoint }}'

# Remove volume (WARNING: deletes all data!)
sudo docker volume rm kabengosafaris_libretranslate_data
```

### Volume Data Location

```
/var/lib/docker/volumes/kabengosafaris_libretranslate_data/_data/
└── packages/           # Language model files
└── cache/              # Cached translations
```

### Backup & Restore Volume

```bash
# Backup volume to tar file
sudo docker run --rm \
  -v kabengosafaris_libretranslate_data:/data \
  -v $(pwd):/backup \
  alpine tar cvf /backup/libretranslate_backup.tar /data

# Restore volume from tar file
sudo docker run --rm \
  -v kabengosafaris_libretranslate_data:/data \
  -v $(pwd):/backup \
  alpine tar xvf /backup/libretranslate_backup.tar -C /
```

---

## 10. Networking

### Default Network Behavior

Docker Compose creates a default network for all services. Containers can communicate using service names as hostnames.

```yaml
# In docker-compose.yml, containers can reach each other by service name:
# libretranslate → http://libretranslate:5000
# mysql → http://mysql:3306
```

### Port Mapping

```yaml
ports:
  - "5000:5000"      # host:container - accessible from outside
  - "127.0.0.1:5000:5000"  # Only accessible from localhost
```

### Network Commands

```bash
# List networks
sudo docker network ls

# Inspect network
sudo docker network inspect kabengosafaris_default

# See which containers are on a network
sudo docker network inspect kabengosafaris_default --format '{{json .Containers}}'
```

---

## 11. Health Checks

### What are Health Checks?

Health checks verify that a container's application is actually working, not just that the container is running.

### Health Check Configuration

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:5000/languages"]
  interval: 30s       # How often to check
  timeout: 10s        # Max time for check to complete
  retries: 5          # Failures before marking unhealthy
  start_period: 180s  # Grace period for startup
```

### Health Status Values

| Status | Description |
|--------|-------------|
| `starting` | Container started, within start_period |
| `healthy` | Health check passing |
| `unhealthy` | Health check failed after retries |

### Check Health Status

```bash
# View health status
sudo docker ps
# Look at STATUS column: "Up 5 minutes (healthy)"

# Detailed health info
sudo docker inspect --format='{{json .State.Health}}' kabengosafaris-libretranslate | jq
```

---

## 12. Resource Management

### Memory & CPU Limits

```yaml
deploy:
  resources:
    limits:
      memory: 4G        # Maximum memory
      cpus: '2'         # Maximum CPU cores
    reservations:
      memory: 2G        # Guaranteed minimum memory
      cpus: '1'         # Guaranteed minimum CPU
```

### View Resource Usage

```bash
# Live resource usage
sudo docker stats

# Resource usage for specific container
sudo docker stats kabengosafaris-libretranslate

# One-time snapshot
sudo docker stats --no-stream
```

### Sample Output

```
CONTAINER ID   NAME                            CPU %   MEM USAGE / LIMIT   MEM %
552a0ccb4cbf   kabengosafaris-libretranslate   0.50%   1.2GiB / 4GiB       30.0%
```

---

## 13. Troubleshooting

### Common Issues & Solutions

#### Container Won't Start

```bash
# Check logs for errors
sudo docker-compose logs libretranslate

# Check container status
sudo docker ps -a

# Remove and recreate
sudo docker-compose down
sudo docker-compose up -d
```

#### Permission Denied

```bash
# Option 1: Use sudo
sudo docker-compose up -d

# Option 2: Add user to docker group
sudo usermod -aG docker $USER
# Then log out and log back in
```

#### Port Already in Use

```bash
# Find what's using port 5000
sudo lsof -i :5000
sudo netstat -tulpn | grep 5000

# Kill the process or change port in docker-compose.yml
```

#### Out of Disk Space

```bash
# Clean up unused Docker resources
sudo docker system prune -a

# Remove unused volumes
sudo docker volume prune

# Check disk usage
sudo docker system df
```

#### Container Keeps Restarting

```bash
# Check logs for crash reason
sudo docker logs kabengosafaris-libretranslate

# Check exit code
sudo docker inspect kabengosafaris-libretranslate --format='{{.State.ExitCode}}'
```

#### No Response from Service

```bash
# Check if container is running
sudo docker ps

# Check if port is exposed
sudo docker port kabengosafaris-libretranslate

# Test from inside container
sudo docker exec -it kabengosafaris-libretranslate curl http://localhost:5000/languages
```

### Debugging Steps

1. **Check container status**: `sudo docker ps -a`
2. **View logs**: `sudo docker-compose logs -f`
3. **Check health**: `sudo docker inspect --format='{{.State.Health.Status}}'`
4. **Shell into container**: `sudo docker exec -it <name> /bin/sh`
5. **Check resources**: `sudo docker stats`

---

## 14. Best Practices

### Development

1. **Use stable image tags** (e.g., `v1.6.1`) instead of `latest`
2. **Don't persist volumes** for faster iterations
3. **Load fewer resources** (fewer languages) for faster startup
4. **Use `.env` files** for environment variables

### Production

1. **Always use volumes** for data persistence
2. **Set resource limits** to prevent memory exhaustion
3. **Use health checks** for monitoring
4. **Restrict network access** (bind to localhost if possible)
5. **Enable restart policies** (`restart: unless-stopped`)
6. **Use specific image versions** for reproducibility
7. **Regular backups** of volume data
8. **Monitor logs** and set up alerting

### Security

1. **Don't run as root** inside containers when possible
2. **Keep images updated** for security patches
3. **Use secrets management** for sensitive data
4. **Limit exposed ports** to only what's necessary
5. **Use read-only file systems** when possible

---

## 15. Quick Reference Cheat Sheet

### Startup

```bash
# Development
sudo docker-compose up -d

# Production
sudo docker-compose -f docker-compose-production.yml up -d
```

### Daily Operations

```bash
# Check status
sudo docker-compose ps

# View logs
sudo docker-compose logs -f libretranslate

# Restart
sudo docker-compose restart

# Stop
sudo docker-compose down
```

### Cleanup

```bash
# Stop and remove containers
sudo docker-compose down

# Stop, remove containers AND volumes (fresh start)
sudo docker-compose down -v

# Clean up everything unused
sudo docker system prune -a
```

### Debugging

```bash
# Logs
sudo docker-compose logs -f

# Shell access
sudo docker exec -it kabengosafaris-libretranslate /bin/sh

# Resource usage
sudo docker stats
```

---

## Environment Variables Reference

### LibreTranslate Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `LT_LOAD_ONLY` | Languages to load | `en,fr,de,es,it` |
| `LT_DISABLE_WEB_UI` | Disable web interface | `true` / `false` |
| `LT_UPDATE_MODELS` | Auto-update models | `true` / `false` |
| `LT_SUGGESTIONS` | Enable suggestions | `true` / `false` |
| `LT_CHAR_LIMIT` | Max characters per request | `10000` |
| `LT_REQ_LIMIT` | Rate limit (requests/min) | `0` (unlimited) |
| `LT_THREADS` | Number of threads | `4` |
| `LT_API_KEY` | API key for authentication | `your-secret-key` |

---

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [LibreTranslate GitHub](https://github.com/LibreTranslate/LibreTranslate)
- [LibreTranslate Docker Hub](https://hub.docker.com/r/libretranslate/libretranslate)
