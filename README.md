<div align="center">

# 🚀 OpsSync — Mobile DevOps Manager

### Monitor. Manage. Mobilize.

**A full-stack DevOps monitoring system built for teams who need real-time pipeline control in their pocket.**

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Backend](https://img.shields.io/badge/Backend-Node.js-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)
![Database](https://img.shields.io/badge/Database-MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Auth](https://img.shields.io/badge/Auth-JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

</div>

---

## 👥 Team

| Name | Role |
|------|------|
| **Krish Kumar Chaurasia** | Team Lead · Android Developer · DevOps Architect |
| **Kanishka Tyagi** | Backend Developer |
| **Kanishka Lodhi** | Backend Developer |
| **Ishita Srivastav** | DevOps / Setup Engineer|

---

## 📌 What is OpsSync?

OpsSync is a **mobile-first DevOps management platform** that brings CI/CD monitoring and incident management directly to your Android device. Built with Kotlin + Jetpack Compose on the frontend and Node.js + Express + MongoDB on the backend, OpsSync connects to GitHub Actions and gives your team full operational control — from anywhere.

---

## ✨ Key Features

| Feature | Description |
|--------|-------------|
| 📊 **Pipeline Dashboard** | Real-time GitHub Actions workflow monitoring |
| 🔁 **Pipeline Control** | Trigger, re-run, or cancel workflow runs remotely |
| 🚨 **Incident Management** | Create, track, and resolve incidents from mobile |
| ⏱ **Auto-Escalation** | Cron-based engine to escalate unresolved incidents |
| 🔔 **Push Notifications** | Firebase-ready notification system |
| 🔐 **Secure Auth** | JWT-based authentication with role-based access |
| 👥 **Role Management** | Separate Admin and Engineer roles |

---

## 🏗️ System Architecture

```
┌─────────────────────────────┐
│   Android App               │
│   (Kotlin + Jetpack Compose)│
└────────────┬────────────────┘
             │ REST API (HTTP/JSON)
             ▼
┌─────────────────────────────┐
│   Node.js + Express API     │
│   (JWT Middleware, Routes)  │
└──────┬──────────────┬───────┘
       │              │
       ▼              ▼
┌──────────┐   ┌──────────────────┐
│ MongoDB  │   │  GitHub Actions  │
│ Database │   │  API (Axios)     │
└──────────┘   └──────────────────┘
```

---

## 📂 Project Structure

```
OpsSync/
│
├── OpsSync-Android/              # 📱 Mobile App (Kotlin + Compose)
│   ├── app/
│   │   ├── ui/                   # Compose screens
│   │   ├── viewmodel/            # ViewModels (MVVM)
│   │   ├── repository/           # Data repositories
│   │   └── network/              # Retrofit API client
│   └── build.gradle
│
└── OpsSync-backend/              # 🌐 Node.js Backend API
    ├── controllers/              # Route handlers
    ├── routes/                   # API routes
    ├── models/                   # Mongoose schemas
    ├── services/                 # Business logic
    ├── middleware/               # JWT auth middleware
    ├── cron/                     # Escalation engine
    ├── .env.example
    └── server.js
```

---

## 📱 Android App

### Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM
- **Networking:** Retrofit + OkHttp
- **Notifications:** Firebase Cloud Messaging (optional)

### App Architecture

```
UI Layer (Compose Screens)
        ↓
ViewModel (State + Business Logic)
        ↓
Repository (Data Abstraction)
        ↓
Retrofit API Client
        ↓
OpsSync Backend Server
```

### Setup

**1. Open in Android Studio**

```
File → Open → OpsSync-Android
```

**2. Configure Backend URL**

In your Retrofit config, set the base URL:

```kotlin
// For Android Emulator
private const val BASE_URL = "http://10.0.2.2:5000/"

// For physical device (replace with your machine's local IP)
// private const val BASE_URL = "http://192.168.x.x:5000/"
```

**3. (Optional) Set up Firebase**

- Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
- Download `google-services.json` and place it in `app/`
- Enable Cloud Messaging

**4. Run**

- Connect an emulator or physical device
- Click ▶ **Run** in Android Studio

---

## 🌐 Backend API

### Tech Stack

- **Runtime:** Node.js
- **Framework:** Express.js
- **Database:** MongoDB + Mongoose
- **Auth:** JWT
- **Scheduler:** node-cron
- **GitHub Integration:** Axios

### Installation

```bash
git clone https://github.com/your-username/OpsSync-backend.git
cd OpsSync-backend
npm install
```

### Environment Variables

Create a `.env` file in the root directory:

```env
# Server
PORT=5000

# Database
MONGO_URI=mongodb://localhost:27017/OpsSync

# Authentication
JWT_SECRET=your_super_secret_key
JWT_EXPIRES_IN=7d

# GitHub Integration
GITHUB_TOKEN=your_personal_access_token
GITHUB_OWNER=your_github_username
GITHUB_REPO=your_repository_name

# Escalation Engine
ESCALATION_THRESHOLD_MINUTES=30

# Notifications
NOTIFICATION_MODE=log   # Use 'firebase' for push notifications
```

> ⚠️ **Never commit your `.env` file.** Add it to `.gitignore`.

### Run the Server

```bash
# Development (with hot reload)
npm run dev

# Production
npm start
```

**Verify it's running:**

```
GET http://localhost:5000/health
```

---

## 🔌 API Reference

### 🔐 Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register new user |
| `POST` | `/api/auth/login` | Login & receive JWT |
| `GET` | `/api/auth/me` | Get current user profile |

### 📊 Pipelines

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/pipelines/workflows` | List all workflows |
| `GET` | `/api/pipelines/runs` | List all workflow runs |
| `POST` | `/api/pipelines/runs/:id/rerun` | Re-run a workflow |
| `POST` | `/api/pipelines/runs/:id/cancel` | Cancel a workflow run |

### 🚨 Incidents

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/incidents` | List all incidents |
| `POST` | `/api/incidents` | Create new incident |
| `PATCH` | `/api/incidents/:id` | Update incident status |

### 🔔 Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/notifications/send` | Send a notification |

---

## ⏱ Escalation Engine

OpsSync includes an automatic incident escalation system powered by `node-cron`:

```
Every minute:
  → Scan for unresolved incidents
  → Check if incident age > ESCALATION_THRESHOLD_MINUTES
  → Auto-assign to admin
  → Send notification
  → Log escalation event
```

The threshold is configurable via the `ESCALATION_THRESHOLD_MINUTES` environment variable.

---

## 🔗 End-to-End Flow

```
User Action (Mobile App)
        ↓
Retrofit HTTP Request
        ↓
Express Route → JWT Middleware
        ↓
Controller → Service Layer
        ↓
MongoDB  ←→  GitHub Actions API
        ↓
JSON Response
        ↓
ViewModel → Compose UI Update
```

---

## 🧪 Testing

| Layer | Tool |
|-------|------|
| Backend API | Postman |
| Android App | Android Emulator / Physical Device |
| Database | MongoDB Compass |

---

## 🚀 Roadmap

- [x] GitHub Actions integration
- [x] Incident management system
- [x] Auto-escalation engine
- [x] JWT role-based auth
- [ ] 🔔 Firebase push notifications
- [ ] 📊 Analytics dashboard
- [ ] 👥 Team collaboration features
- [ ] 📡 Real-time updates via WebSockets
- [ ] 🌍 Cloud deployment (AWS / Render / Vercel)

---

## 🔒 Security Notes

- Backend `.env` is **never committed** to version control
- GitHub personal access token must remain **private**
- JWTs expire after the configured `JWT_EXPIRES_IN` duration
- All protected routes require a valid `Authorization: Bearer <token>` header

---

## 📄 License

This project was built as a team project. All rights reserved by the OpsSync team.

---

<div align="center">

Built with ❤️ by the **OpsSync Team**

**Krish Kumar Chaurasia · Kanishka Tyagi · Kanishka Lodhi · Ishita Srivastav**

</div>
