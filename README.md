# OpsSync – Mobile DevOps Manager

A Node.js + Express backend that acts as an API layer between a mobile app and DevOps tools like GitHub Actions. It lets you monitor pipelines, manage incidents, handle escalations, and send notifications — all secured with JWT authentication.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Environment Variables](#environment-variables)
- [Running the Project](#running-the-project)
- [API Reference](#api-reference)
- [Testing with Postman](#testing-with-postman)
- [How It Works](#how-it-works)

---

## Overview

OpsSync allows you to:

- Monitor GitHub Actions pipeline runs in real time
- Re-run or cancel workflow runs
- Create and manage incidents with severity levels
- Auto-escalate unresolved incidents after a time threshold
- Assign incidents to team members
- Send notifications (console log in dev, Firebase-ready for production)
- Authenticate users using JWT

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Node.js |
| Framework | Express.js |
| Database | MongoDB + Mongoose |
| Authentication | JSON Web Tokens (JWT) |
| Password Hashing | bcryptjs |
| GitHub Integration | Axios + GitHub REST API |
| Background Jobs | node-cron |
| Security | helmet, cors |
| Logging | morgan |

---

## Project Structure

```
OpsSync-backend/
│
├── server.js                   # Entry point
├── .env                        # Environment variables (you create this)
├── .env.example                # Template for .env
├── package.json
│
├── config/
│   ├── db.js                   # MongoDB connection
│   ├── github.js               # GitHub API client (Axios)
│   └── jwt.js                  # JWT sign & verify helpers
│
├── middleware/
│   ├── auth.js                 # JWT guard (protect + restrictTo)
│   └── errorHandler.js         # Global error handler
│
├── models/
│   ├── User.js                 # User schema (bcrypt hashing built in)
│   ├── Incident.js             # Incident schema (severity, status, assignee)
│   └── Escalation.js           # Escalation audit log schema
│
├── services/
│   ├── github.service.js       # All GitHub Actions API calls
│   ├── escalation.service.js   # Escalation engine + cron job
│   └── notification.service.js # Notification sender
│
├── controllers/
│   ├── auth.controller.js
│   ├── pipeline.controller.js
│   ├── incident.controller.js
│   ├── escalation.controller.js
│   └── notification.controller.js
│
└── routes/
    ├── auth.routes.js
    ├── pipeline.routes.js
    ├── incident.routes.js
    ├── escalation.routes.js
    └── notification.routes.js
```

---

## Prerequisites

Make sure you have the following installed before running the project:

- **Node.js** v18 or higher → https://nodejs.org
- **MongoDB** running locally → https://www.mongodb.com/try/download/community
- **Git** → https://git-scm.com
- **Postman** (for testing) → https://www.postman.com

---

## Installation

**Step 1 — Clone the repository**

```bash
git clone https://github.com/your-username/OpsSync-backend.git
cd OpsSync-backend
```

**Step 2 — Install dependencies**

```bash
npm install
```

This installs everything listed in `package.json`. You do not need to install packages one by one.

**Step 3 — Create your `.env` file**

```bash
cp .env.example .env
```

Then open `.env` and fill in your values (see Environment Variables section below).

---

## Environment Variables

Open `.env` and fill in the following:

```dotenv
# Server
PORT=5000
NODE_ENV=development

# MongoDB — make sure MongoDB is running locally
MONGO_URI=mongodb://localhost:27017/OpsSync

# JWT — use any long random string as your secret
JWT_SECRET=your_super_secret_key_change_this
JWT_EXPIRES_IN=7d

# GitHub — see instructions below
GITHUB_TOKEN=ghp_your_personal_access_token
GITHUB_OWNER=your_github_username
GITHUB_REPO=your_repository_name

# Escalation threshold in minutes
ESCALATION_THRESHOLD_MINUTES=30

# Notification mode: 'log' prints to terminal, 'firebase' sends push notifications
NOTIFICATION_MODE=log
```

### How to get your GitHub values

**`GITHUB_TOKEN`**
1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click Generate new token (classic)
3. Check `repo` and `workflow` scopes
4. Copy the token — GitHub only shows it once

**`GITHUB_OWNER`**
Your GitHub username or organization name. Visible in any repo URL:
`github.com/OWNER/repo-name`

**`GITHUB_REPO`**
The name of the repo you want to monitor. The repo must have at least one GitHub Actions workflow.
`github.com/owner/REPO-NAME`

---

## Running the Project

**Development mode** (auto-restarts on file changes):

```bash
npm run dev
```

**Production mode:**

```bash
npm start
```

**Expected terminal output on success:**

```
✅ OpsSync server running on port 5000
   Environment : development
   Health check: http://localhost:5000/health
✅ MongoDB connected: localhost
⏱  Escalation cron job started (runs every minute)
```

**Verify the server is running:**

Open your browser and go to:
```
http://localhost:5000/health
```

You should see:
```json
{ "status": "ok", "project": "OpsSync" }
```

---

## API Reference

### Authentication

All routes except `/register` and `/login` require a JWT token in the header:

```
Authorization: Bearer <your_token>
```

---

### Auth Routes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Create a new account |
| POST | `/api/auth/login` | No | Login and get a token |
| GET | `/api/auth/me` | Yes | Get current user |

**Register body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "123456",
  "role": "engineer"
}
```

**Login body:**
```json
{
  "email": "john@example.com",
  "password": "123456"
}
```

---

### Pipeline Routes

All pipeline routes require authentication.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/pipelines/workflows` | List all workflows in your repo |
| GET | `/api/pipelines/runs` | Get recent runs across all workflows |
| GET | `/api/pipelines/workflows/:workflowId/runs` | Get runs for one workflow |
| GET | `/api/pipelines/runs/:runId` | Get details for one run |
| POST | `/api/pipelines/runs/:runId/rerun` | Re-trigger a failed run |
| POST | `/api/pipelines/runs/:runId/cancel` | Cancel an in-progress run |
| POST | `/api/pipelines/runs/:runId/create-incident` | Create an incident from a failed run |

---

### Incident Routes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/incidents` | Yes | List all incidents (filterable) |
| GET | `/api/incidents/:id` | Yes | Get one incident |
| POST | `/api/incidents` | Yes | Create an incident |
| PATCH | `/api/incidents/:id` | Yes | Update title/severity/status |
| PATCH | `/api/incidents/:id/assign` | Yes | Assign to a user |
| DELETE | `/api/incidents/:id` | Admin only | Delete an incident |

**Create incident body:**
```json
{
  "title": "Pipeline failed on main branch",
  "description": "Build broke after merge",
  "severity": "high",
  "assignedTo": "user_id_here"
}
```

**Severity values:** `low` | `medium` | `high` | `critical`

**Status transitions:** `open` → `in_progress` → `resolved`

**Filter examples:**
```
GET /api/incidents?status=open
GET /api/incidents?severity=critical
GET /api/incidents?sortBy=severity
```

---

### Escalation Routes

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/escalations` | View escalation audit log |
| GET | `/api/escalations/:id` | Get one escalation record |
| POST | `/api/escalations/:incidentId/escalate` | Manually escalate an incident |

---

### Notification Routes

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/notifications/send` | Yes | Send to one user |
| POST | `/api/notifications/broadcast` | Admin only | Send to all users |

**Send notification body:**
```json
{
  "userId": "user_id_here",
  "title": "Alert",
  "message": "Your incident was escalated"
}
```

---

## Testing with Postman

### Step 1 — Register a user

```
POST http://localhost:5000/api/auth/register
Body (raw JSON):
{
  "name": "John",
  "email": "john@example.com",
  "password": "123456"
}
```

Copy the `token` value from the response.

### Step 2 — Set up the token in Postman

1. Open any protected request
2. Click the **Auth** tab
3. Select **Bearer Token** from the dropdown
4. Paste your token

### Step 3 — Test protected routes

```
GET http://localhost:5000/api/auth/me
Authorization: Bearer <your_token>
```

### Step 4 — Create an incident

```
POST http://localhost:5000/api/incidents
Authorization: Bearer <your_token>
Body:
{
  "title": "Build failed on main",
  "severity": "high"
}
```

### Step 5 — Test GitHub pipeline routes

Make sure `GITHUB_TOKEN`, `GITHUB_OWNER`, and `GITHUB_REPO` are set in `.env`, then:

```
GET http://localhost:5000/api/pipelines/workflows
Authorization: Bearer <your_token>
```

This returns the real workflows from your GitHub repo.

---

## How It Works

### Request flow

```
Mobile App / Postman
        ↓
    auth.js middleware  ← validates JWT token
        ↓
    routes/*.routes.js  ← maps URL to controller
        ↓
    controllers/*.js    ← handles request/response
        ↓
    services/*.js       ← business logic
        ↓
  MongoDB / GitHub API  ← data source
```

### Escalation engine

A background cron job runs every minute. It finds any incident that is still `open` or `in_progress` and was created more than `ESCALATION_THRESHOLD_MINUTES` ago. It then:

1. Reassigns the incident to the first admin user
2. Creates an Escalation audit record
3. Sends a notification to the new assignee

### Security

- Passwords are hashed with bcrypt before storing — plain passwords never touch the database
- JWT secret lives only in `.env` — never sent to the client
- GitHub token lives only in `.env` — never exposed in API responses
- `helmet` adds secure HTTP headers to every response
- `restrictTo('admin')` protects destructive endpoints

---

## Common Errors

| Error | Cause | Fix |
|---|---|---|
| `MongoDB connection error` | MongoDB not running | Start MongoDB locally |
| `No token provided` | Missing Authorization header | Add `Bearer <token>` to headers |
| `Invalid or expired token` | Token expired or wrong | Login again to get a new token |
| `Cannot transition from open to resolved` | Wrong status order | Update to `in_progress` first |
| `count: 0, workflows: []` | Repo has no workflows | Add a `.github/workflows/*.yml` file |

---

## Notes

- The `.env` file is listed in `.gitignore` — it will never be committed to GitHub
- `NOTIFICATION_MODE=log` means notifications print to your terminal — no external service needed
- To enable real push notifications, set `NOTIFICATION_MODE=firebase` and plug in Firebase Admin SDK in `notification.service.js`
- The escalation job only runs while the server is running

## 🐳 Running with Docker

# Build image
docker build -t backend-app .

# Run container
docker run -p 3000:3000 backend-app
