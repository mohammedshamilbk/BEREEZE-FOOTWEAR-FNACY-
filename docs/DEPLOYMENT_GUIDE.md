# Deployment Guide — Bereeze Footwear Fancy Cloud Stack

## Overview

This guide details deploying the enterprise-grade Bereeze Footwear Fancy Cloud application using Docker Compose, PostgreSQL 15, FastAPI, Redis, and Nginx.

---

## Prerequisites

1. **Server Spec**: VPS / Cloud instance (Ubuntu 22.04 LTS recommended, min 2GB RAM, 2 vCPUs).
2. **Tools**: Docker and Docker Compose installed.

---

## Step-by-Step Production Deployment

### 1. Clone Codebase & Configure Environment
```bash
git clone https://github.com/mohammedshamilbk/Bereezefootwearfancy.git
cd Bereezefootwearfancy

cp .env.example .env
```
Edit `.env` to set a strong `SECRET_KEY` and production credentials.

### 2. Launch Stack with Docker Compose
```bash
docker-compose up -d --build
```

### 3. Verify Container Health
```bash
docker-compose ps
```
You should see:
- `bereeze_pos_app` (Up / Healthy)
- `bereeze_pos_postgres` (Up / Healthy)
- `bereeze_pos_redis` (Up)
- `bereeze_pos_nginx` (Up)

### 4. SSL Certificate Setup (Certbot / Nginx)
To enable HTTPS, obtain a Let's Encrypt SSL certificate:
```bash
sudo apt-get install certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

### 5. Access Application
- **Web App**: `https://your-domain.com`
- **Swagger API Docs**: `https://your-domain.com/docs`
