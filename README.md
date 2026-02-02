# Numbers Don't Lie — Meal Planning & Wellness Platform

A full‑stack meal planning and wellness application built with **Spring Boot**, **PostgreSQL**, and a **React (Vite)** frontend. It supports daily and weekly meal plans, meal replacement, custom meals, AI‑assisted features, nutrition summaries, shopping lists, progress charts, and versioned weekly plans.

This README covers:
- Project overview and architecture
- Setup and run instructions
- Usage guide
- Prompt engineering strategy
- AI model selection rationale
- Data model decisions
- Error handling approach

---

## 1) Project Overview

Core capabilities:
- Daily and weekly meal plans (profile‑driven)
- Meal replacement and custom meals
- Weekly plan versioning (regenerate + restore)
- Nutrition summaries and progress visualizations
- Shopping lists (daily and weekly)
- AI‑assisted recipe generation and nutrition insights

---

## 2) Architecture

**Backend**
- Java 17+, Spring Boot 3+
- PostgreSQL
- REST API endpoints for meal plans, recipes, AI, shopping lists, and analytics

**Frontend**
- React + Vite
- Fetch API for backend communication
- Pages for Today Plan, Weekly Plan, Recipes, Shopping Lists, Progress Charts

**Deployment**
- Docker Compose (backend + frontend + PostgreSQL)

---

## 3) Setup & Run

### Prerequisites
- Docker + Docker Compose

### Environment
Copy example and fill secrets:
```bash
cp .env.example .env
```

Key variables:
- `GROQ_API_KEY` (AI features)
- `FRONTEND_ORIGIN` (default: http://localhost:5173)

### Run with Docker
```bash
docker compose up -d --build
```

Services:
- Frontend: http://localhost:5173
- Backend:  http://localhost:5173 (proxied)
- Health:   http://localhost:5173/health

---

## 4) Usage Guide (Quick)

### Daily Plan
- View meals for today
- Replace a meal with another recipe
- Add custom meals
- Generate AI recipes
- See daily nutrition summary + AI insights

### Weekly Plan
- View week (Monday → Sunday)
- Regenerate weekly plan (creates new version)
- Restore previous version
- Replace meals / add custom meals
- Weekly nutrition summaries and trends

### Shopping List
- Daily shopping list: `/shopping-list/day`
- Weekly shopping list: `/shopping-list/week`

### Progress Charts
- Weekly and monthly calorie trends
- AI weekly insights (if API key available)

---

## 5) Prompt Engineering Strategy

AI flows are structured as **multi‑step prompts** to improve consistency:
1. **Strategy analysis** — determine user goals, calorie targets, and overall plan direction.
2. **Meal structure** — decide meal slots and per‑meal calorie distribution.
3. **Recipe generation** — generate or retrieve recipes based on constraints and targets.

This separation improves control and reliability compared to a single prompt.

---

## 6) AI Model Selection Rationale

- **Recipe generation** uses a model tuned for creative text + structured JSON output.
- **Nutrition insights** use a model optimized for concise, factual summaries.

Rationale:
- Recipe generation benefits from creativity and variety.
- Nutrition insights require precision and stable tone.

(Models are configurable via the Groq client and environment keys.)

---

## 7) Data Model Decisions (Highlights)

### Meal Plans
- `MealPlan` (weekly/daily) owns `MealPlanVersion`
- `MealPlanVersion` contains 7 `DayPlan` rows for a week
- `DayPlan` owns `Meal` records

### Meals
- `Meal` stores type, planned time, recipe id, and calories
- `is_custom` marks user‑added meals

### Recipes
- Recipes store ingredients + steps
- AI‑generated recipes are flagged in DB (`is_ai_generated`)

### Shopping Lists
- Generated on‑demand (no persistence)
- Aggregates ingredients by name + unit

---

## 8) Error Handling

### Backend
- Input validation (missing/invalid params → 400)
- Safe fallbacks (empty lists instead of 500)
- Clear error JSON payloads
- AI failures handled without blocking core features

### Frontend
- Loading + error states per page
- Graceful fallbacks if AI endpoints fail
- No blocking UI if AI unavailable

---

## 9) API Quick Reference

Examples:
- `GET /api/meal-plans/day?userId=...&date=YYYY-MM-DD`
- `GET /api/meal-plans/week?userId=...&startDate=YYYY-MM-DD`
- `POST /api/meal-plans/week/refresh?userId=...&startDate=YYYY-MM-DD`
- `GET /api/meal-plans/week/versions?userId=...&startDate=YYYY-MM-DD`
- `POST /api/meal-plans/week/versions/restore?...`
- `GET /api/shopping-list/day?userId=...&date=YYYY-MM-DD`
- `GET /api/shopping-list/week?userId=...&startDate=YYYY-MM-DD`

---

## 10) Notes

- AI features require `GROQ_API_KEY`.
- Meal plan versions are immutable snapshots; regenerating creates a new version.
- Week always runs Monday → Sunday.

---

## 11) Old README

Previous version preserved as `README_OLD.md`.
