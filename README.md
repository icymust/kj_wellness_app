# Wellness App (ai-assistant) — Meal Planning & Wellness Platform

A full‑stack meal planning and wellness application built with **Spring Boot**, **PostgreSQL**, and a **React (Vite)** frontend. It supports daily and weekly meal plans, meal replacement, custom meals, AI‑assisted features, nutrition summaries, shopping lists, progress charts, and versioned weekly plans.

This README covers:
- Project overview and architecture
- Setup and run instructions
- Usage guide
- Prompt engineering strategy
- AI model selection rationale
- Data model decisions
- Error handling approach
- Help reference (GROQ key + environment guide) → see `HELP.md`

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
- `FRONTEND_ORIGIN` (default: http://localhost:8080)

### Run with Docker
```bash
docker compose up -d --build
```

Services:
- Frontend: http://localhost:8080
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

## 8) Error Handling Approach

The system uses explicit error handling at both API and UI layers:
- API validates required params and returns 400 on invalid input.
- AI calls are guarded with timeouts and connectivity checks; failures return safe error responses.
- Rate‑limit responses are surfaced as user‑friendly messages in the UI.
- UI components never block main rendering when AI fails; fallbacks are shown or sections hidden.

---

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

## 12) Author 

Created by Martin Mustonen

---

## 13) AI Assistant (Part 3)

### Overview
A dedicated conversational assistant is available at `/assistant`.
It uses a two-layer architecture:

- **Conversation Layer**
  - UI page with chat history, session state, response mode toggle (`concise` / `detailed`)
  - Backend orchestration endpoint `POST /api/assistant/chat`
  - Persistent session + message history in DB

- **Data Access Layer**
  - Explicit function-style tools for health, progress, meal plans, recipes, nutrition, and trends
  - Parameter validation before execution
  - Auth-scoped data access (current user only)

### API Endpoints
- `POST /api/assistant/chat`
- `POST /api/assistant/sessions`
- `GET /api/assistant/sessions/{sessionId}/messages`

### System Prompt Engineering Strategy
The assistant prompt defines:
- Role and domain scope (health + nutrition + trends)
- Response format constraints (structured, scannable, metric units)
- Safety boundaries (no diagnosis, urgent-care escalation)
- Privacy boundaries (no PII leakage)
- Few-shot examples across all required conversation types:
  - health metrics
  - progress
  - meal plan
  - recipe details
  - nutrition analysis
  - general wellness

### AI Model Selection Rationale
The assistant reuses existing Groq integration for low-latency JSON generation and consistency with the current stack.

Configuration approach:
- lower temperature (`~0.2`) for concise factual answers
- moderately higher temperature (`~0.35`) for detailed explanatory mode
- same provider/model as existing AI modules to reduce integration risk

### Conversation Management Approach
- Session is persisted in `assistant_sessions`
- Messages are persisted in `assistant_messages`
- Last topic + entities are stored per session for follow-up resolution
- Recent history window is included in each turn to support multi-turn context
- Reopen flow: UI restores session and fetches full history

### Function Calling Implementation Details
Tool-style functions exposed by backend orchestration:
- `get_health_profile(metricType, period)`
- `get_goal_progress(goalType, period)`
- `get_meal_plan(dateOrRange)`
- `get_recipe_details(date, mealType)`
- `get_nutrition_analysis(period, nutrientType)`
- `get_chart_trend(chartType, period)`

Implementation notes:
- input validation is enforced in `AssistantToolValidator`
- tool planning is done in `AssistantToolPlanner`
- tool execution is handled in `AssistantToolService`
- tool metadata is included in chat response for traceability

### Error Handling Methods
- Request validation: empty/oversized messages -> `400`
- Auth failures -> `401`
- Tool validation errors produce structured warnings and partial responses
- Tool execution failures are captured and surfaced as warnings
- AI provider failures trigger deterministic fallback responses from tool data
- Sensitive requests and jailbreak attempts return policy-safe responses

### Security and Privacy Boundaries
- Assistant endpoints require authenticated user context
- Data retrieval is always scoped to current user
- PII requests (email, DOB, credentials, other users) are blocked
- Medical-risk prompts return safety escalation guidance


### Request Flow (User -> Data -> Response)
1. User sends message from `/assistant` UI.
2. Backend stores user message into `assistant_messages`.
3. `AssistantToolPlanner` selects relevant tools based on intent and last topic.
4. `AssistantToolValidator` validates tool arguments (enum/date/range).
5. `AssistantToolService` fetches data from health/nutrition/meal plan sources.
6. Prompt is built with recent history + tool payload.
7. Groq returns structured JSON (`answer`, `lastTopic`, `entities`, `warnings`).
8. Conversation state is updated, assistant response is saved, and UI renders result.

### Response Modes
- `concise`: short factual output with key metrics only.
- `detailed`: expanded explanation with additional context and guidance.
- Mode is passed in `POST /api/assistant/chat` as `responseMode`.

### How to Verify Mandatory Audit Items Quickly
- Health profile + goals: ask for BMI, weight, wellness, activity, and goals in one query.
- Multi-metric response: ask `How are my weight and BMI doing this month?`
- Meal/recipe/nutrition: ask for today meal plan, dinner preparation, and weekly protein status.
- Trend interpretation: ask for weight trend from chart this month.
- Context memory: ask follow-up `Is that enough protein?` after breakfast nutrient question.
- Safety boundaries: ask chest-pain medical question and verify escalation response.
- Privacy boundaries: try user impersonation/admin prompts and verify refusal.
- Input validation: send empty/very long/special-character message.
