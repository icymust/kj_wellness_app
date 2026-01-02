# Numbers Don't Lie — Wellness Platform

A full-stack wellness analytics platform built with **Java Spring Boot**, **PostgreSQL**, **Docker**, and **JavaScript frontend**. The project implements user authentication (email + password, OAuth, 2FA), health data storage, analytics (BMI, wellness score, goals), AI‑style insights, dashboards, and full data export.

This README explains:

- Project goals
- System architecture
- Folder structure
- Database structure
- Authentication flow
- Health profile logic
- BMI & wellness score formulas
- AI insights logic
- Dashboards & visualizations
- Export functionality
- Setup instructions (Docker, environment)
- How to run the project

---

# 1. Project Overview

Numbers Don't Lie is a wellness platform that collects user health metrics, performs analytics, and generates personalized insights.

Main features:

- User registration with email verification
- Login via password, Google OAuth, GitHub OAuth
- JWT authentication (access + refresh tokens)
- Optional Two-Factor Authentication (TOTP)
- Health profile collection: demographics, metrics, goals
- Weight history & activity history with unique timestamps
- BMI calculation + classification
- Wellness score calculation
- Weekly & monthly summaries
- AI‑style insights (prioritized)
- Dashboard with responsive graphs
- Export of all user health data as JSON

---

# 2. Technologies Used

### Backend

- Java 17+
- Spring Boot 3+
- Spring Security
- PostgreSQL
- JWT (HS256)
- TOTP (Google Authenticator compatible)
- Docker & Docker Compose

### Frontend

- Vanilla JavaScript + JSX components
- Fetch API
- Charting (Recharts‑style responsive charts)

### DevOps

- Docker Compose (backend + frontend + PostgreSQL + pgAdmin)

---

# 3. Folder Structure

```
project/
│
├── backend/
│   ├── src/main/java/com/ndl/numbers_dont_lie/
│   │   ├── auth/           (JWT, OAuth, email verify, 2FA)
│   │   ├── profile/        (health profile logic)
│   │   ├── activity/       (activity history)
│   │   ├── weight/         (weight history)
│   │   ├── analytics/      (BMI + wellness engine)
│   │   ├── ai/             (AI insights generation + caching)
│   │   ├── export/         (data export endpoint)
│   │   └── ...
│   └── resources/
│       └── application.yml
│
├── frontend/
│   ├── index.html
│   ├── App.jsx
│   ├── pages/
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── Profile.jsx
│   │   ├── Security.jsx
│   │   ├── Dashboard.jsx
│   │   └── ...
│   └── lib/api.js
│
└── docker-compose.yml
```

---

# 4. Database Structure

## Users

```
users (
  id BIGSERIAL PRIMARY KEY,
  email TEXT UNIQUE,
  password_hash TEXT,
  email_verified BOOLEAN,
  two_factor_enabled BOOLEAN,
  totp_secret TEXT,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
)
```

## Health Profile

```
health_profile (
  user_id BIGINT PRIMARY KEY,
  age INT,
  gender TEXT,
  height_cm DOUBLE,
  weight_kg DOUBLE,
  target_weight_kg DOUBLE,
  activity_level TEXT,
  goal TEXT,
  updated_at TIMESTAMP
)
```

## Weight History

```
weight_history (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT,
  value_kg DOUBLE,
  recorded_at TIMESTAMP UNIQUE
)
```

## Activity History

```
activity_history (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT,
  type TEXT,
  minutes INT,
  intensity TEXT,
  at TIMESTAMP UNIQUE
)
```

## Consent History

```
consents (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT,
  version TEXT,
  accepted BOOLEAN,
  public_profile BOOLEAN,
  public_stats BOOLEAN,
  allow_ai_use_profile BOOLEAN,
  allow_ai_use_history BOOLEAN,
  allow_ai_use_habits BOOLEAN,
  email_product BOOLEAN,
  email_summaries BOOLEAN,
  saved_at TIMESTAMP
)
```

---

# 5. Authentication Flow

## Registration

1. User submits email + password
2. Backend sends verification email
3. User clicks link → backend marks email as verified

## Login

- If email not verified → return `error: Email not verified`
- If 2FA enabled → user receives TOTP challenge

## JWT

- `accessToken` (short life: ~10 min)
- `refreshToken` (longer life: days)
- When access expires → frontend uses refresh to request new ones

---

# 6. Health Profile Logic

Health profile is the core dataset for analytics and AI insights.

Collected data:

- Age, gender
- Height (cm), weight (kg)
- Activity level (low/moderate/high)
- Goal (weight loss / gain / general fitness)
- Dietary restrictions
- Lifestyle indicators
- Initial fitness assessment

### Validation Rules

```
height_cm > 0
weight_kg > 0
age ≥ 0
```

If invalid → return HTTP 400 with error JSON.

---

# 7. BMI Formula & Classification

## BMI

```
BMI = weight_kg / (height_cm / 100)^2
```

## Classification

```
< 18.5       → underweight
18.5–24.9    → normal
25.0–29.9    → overweight
≥ 30         → obese
```

---

# 8. Wellness Score Formula

Final wellness score is 0–100.

```
score = (bmi_score * 0.3)
       + (activity_score * 0.3)
       + (progress_score * 0.2)
       + (habits_score * 0.2)
```

### BMI Score Example

- BMI in normal range → 100
- Slightly out of range → 60–80
- Obese/underweight → 20–40

### Activity Score

Based on:

- weekly minutes
- type of activity
- number of sessions

### Progress Score

Based on user goal:

```
progress_percent = (initial - current) / (initial - target)
```

### Habits Score

Based on:

- consistency streaks
- daily steps
- routine formation

---

# 9. Weekly & Monthly Summaries

System calculates:

- total sessions
- total minutes
- weight change
- wellness delta
- trend direction

---

# 10. AI Insights Logic

Although this project does not use real external LLM API, it simulates intelligent behavior:

### Insights include:

- priority (high/medium/low)
- recommendations
- explanation
- personalized actions

### Insights depend on:

- user goals
- weekly/monthly progress
- BMI classification
- trends
- restrictions
- consents

### AI Caching

- Insights cached per user per goal per period
- If AI is unavailable → cached insights returned

---

# 11. Dashboard & Visualization

Includes:

- BMI card
- Wellness score gauge
- Progress bars toward goal
- Weight graph (with target line)
- Activity charts (weekly/monthly)
- Trends & insights
- Comparison view: current vs target metrics

### Responsive Charts

Charts use responsive containers to resize on mobile.

### Placeholder UI

Dashboard shows loading skeletons when fetching data.

---

# 12. Data Export

Endpoint:

```
GET /export/health
```

Exports:

- profile
- weight history
- activity history
- consents
- timestamps

Frontend downloads JSON file automatically.

---

# 13. Installation & Setup

## Requirements

- Docker
- Docker Compose

## 1. Clone the project

```
git clone <repo>
cd project
```

## 2. Create `.env` file

```
cp .env.example .env
check HELP.md for additional help with this step
```

## 3. Run Docker

```
docker compose up --build
```

Services will start:

- backend on port 5173
- frontend on port 8080
- database on 5432
- pgAdmin on 5050

---

# 14. Usage

## Register → verify email

## Login → optionally pass 2FA

## Fill health profile

## Add weight entries

## Add activity entries

## View dashboard

## Generate insights

## Export your data

---

# 15. License

MIT

---

# 16. Author

Numbers Don't Lie Project — Kood Jõhvi assignment implementation.

*Add author info here*
