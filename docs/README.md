# Documentation Index

Welcome! This folder contains all documentation for the Notification Service. Choose your path below:

## 🚀 Quick Start (5 minutes)

**New to the project?** Start here:
- [Quick Deploy Guide](deployment/QUICK_DEPLOY.md) - Get running in 5 minutes
- [Quick Setup](deployment/SETUP.md) - Local development setup

## 🐳 Docker & Deployment

Need to containerize or deploy?
- [Docker Guide](docker/OVERVIEW.md) - Docker fundamentals
- [Deployment Workflow](deployment/WORKFLOW.md) - Step-by-step deployment
- [Production Checklist](deployment/CHECKLIST.md) - Pre/post deployment verification

## 📚 Reference & Commands

Looking for specific commands?
- [Docker Commands](reference/COMMANDS.md) - Copy-paste ready commands
- [Environment Variables](reference/ENVIRONMENT.md) - Complete config reference
- [Troubleshooting](reference/TROUBLESHOOTING.md) - Common issues & fixes

## 🏗️ Architecture & Design

Understanding the system:
- [System Overview](ARCHITECTURE.md) - High-level architecture
- [Security Practices](SECURITY.md) - Security guidelines (root level)
- [Technology Stack](STACK.md) - Tools & frameworks

## 📖 Project Files

Essential files in root:
- [README.md](../README.md) - Project overview
- [ENV_VARIABLES.md](../ENV_VARIABLES.md) - Detailed environment config
- [SECURITY.md](../SECURITY.md) - Security best practices

## 🛠️ Build & Scripts

Automation tools:
- [build-and-push.ps1](../build-and-push.ps1) - PowerShell build script
- [docker/Dockerfile](../docker/Dockerfile) - Container definition
- [docker-compose files](../docker/) - Local & production setup

---

## 📁 Documentation Structure

```
docs/
├── README.md                      (this file - start here!)
├── ARCHITECTURE.md                (system design overview)
├── STACK.md                       (tech stack details)
│
├── deployment/                    (deploy to production)
│   ├── QUICK_DEPLOY.md           (5-min quickstart)
│   ├── SETUP.md                  (local development)
│   ├── WORKFLOW.md               (full deployment workflow)
│   ├── CHECKLIST.md              (verification steps)
│   └── TROUBLESHOOTING.md        (common issues)
│
├── docker/                        (container & orchestration)
│   ├── OVERVIEW.md               (docker fundamentals)
│   ├── BUILD.md                  (building images)
│   ├── SECURITY.md               (container security)
│   └── COMPOSE.md                (docker-compose details)
│
└── reference/                     (quick lookup)
    ├── COMMANDS.md               (docker/compose commands)
    ├── ENVIRONMENT.md            (env vars reference)
    └── TROUBLESHOOTING.md        (error solutions)
```

---

## ✨ Key Features

- **Multi-channel notifications** - Email & WhatsApp
- **Type-safe enums** - Channel, TemplateType, NotificationStatus
- **Async processing** - RabbitMQ integration
- **Production-ready** - Security hardened, health checks
- **Fully containerized** - Docker & Docker Compose ready

---

## 🎯 Choose Your Path

| If you want to... | Go to... |
|---|---|
| **Deploy in 5 minutes** | [Quick Deploy Guide](deployment/QUICK_DEPLOY.md) |
| **Set up locally** | [Setup Guide](deployment/SETUP.md) |
| **Deploy to production** | [Deployment Workflow](deployment/WORKFLOW.md) |
| **Verify deployment** | [Deployment Checklist](deployment/CHECKLIST.md) |
| **Understand the architecture** | [System Overview](ARCHITECTURE.md) |
| **Find a command** | [Docker Commands](reference/COMMANDS.md) |
| **Configure environment** | [Environment Variables](reference/ENVIRONMENT.md) |
| **Fix an issue** | [Troubleshooting](reference/TROUBLESHOOTING.md) |
| **Learn the tech stack** | [Technology Stack](STACK.md) |

---

## 🚀 Getting Started Now

### 1. For Local Development
```bash
cd docs/deployment
cat SETUP.md
```

### 2. For Docker Deployment
```bash
cd docs/deployment
cat QUICK_DEPLOY.md
```

### 3. For Production
```bash
cd docs/deployment
cat WORKFLOW.md
```

---

## ❓ Still Lost?

- 📖 Check the [Architecture Guide](ARCHITECTURE.md) first
- 🔍 Search for keywords in [Troubleshooting](reference/TROUBLESHOOTING.md)
- 📝 Browse [Environment Variables](reference/ENVIRONMENT.md) for configuration
- 🐳 Review [Docker Overview](docker/OVERVIEW.md) for containerization

---

**Last Updated:** January 31, 2026  
**Status:** ✅ All documentation current
