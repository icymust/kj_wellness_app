# 📊 Database Schema Documentation

## Database Overview

- **Database Name:** `ndl`
- **DBMS:** PostgreSQL 16.11
- **Total Tables:** 12
- **Last Updated:** 2026-01-19

---

## Core Tables

### 1. `users`
**Purpose:** Stores user account information

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | User ID |
| username | VARCHAR | NOT NULL, UNIQUE | Username |
| email | VARCHAR | NOT NULL, UNIQUE | Email address |
| password_hash | VARCHAR | NOT NULL | Hashed password |
| created_at | TIMESTAMP | DEFAULT NOW() | Account creation date |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update date |

**Relationships:**
- 1:1 with `profiles`
- 1:1 with `nutritional_preferences`
- 1:N with `activity_entries`
- 1:N with `weight_entries`
- 1:N with `password_reset_tokens`
- 1:1 with `user_consents`

---

### 2. `profiles`
**Purpose:** Stores user health and fitness profile information

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Profile ID |
| user_id | BIGINT | UNIQUE, FOREIGN KEY | Reference to users table |
| age | INTEGER | 5..120 | User age in years |
| gender | VARCHAR(16) | male\|female\|other | User gender |
| height_cm | INTEGER | 50..300 | Height in centimeters |
| weight_kg | DOUBLE | 20..500 | Current weight in kilograms |
| target_weight_kg | DOUBLE | - | Target weight goal in kg |
| activity_level | VARCHAR(24) | low\|moderate\|high | Physical activity level |
| goal | VARCHAR(32) | weight_loss\|muscle_gain\|general_fitness | Fitness goal |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation date |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update date |

**Relationships:**
- 1:1 with `users` (via user_id)

---

### 3. `nutritional_preferences`
**Purpose:** Main table for storing all nutritional preferences and meal timing for users

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | PRIMARY KEY, UNIQUE, FK | Reference to users table |
| calorie_target | INTEGER | - | Daily calorie target (kcal) |
| protein_target | INTEGER | - | Daily protein target (grams) |
| carbs_target | INTEGER | - | Daily carbs target (grams) |
| fats_target | INTEGER | - | Daily fats target (grams) |
| breakfast_count | INTEGER | 0-5, DEFAULT 1 | Number of breakfasts per day |
| lunch_count | INTEGER | 0-5, DEFAULT 1 | Number of lunches per day |
| dinner_count | INTEGER | 0-5, DEFAULT 1 | Number of dinners per day |
| snack_count | INTEGER | 0-5, DEFAULT 0 | Number of snacks per day |
| breakfast_time | VARCHAR(5) | HH:mm format | Breakfast time |
| lunch_time | VARCHAR(5) | HH:mm format | Lunch time |
| dinner_time | VARCHAR(5) | HH:mm format | Dinner time |
| snack_time | VARCHAR(5) | HH:mm format | Snack time |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update date |

**Relationships:**
- 1:1 with `users` (via user_id)
- 1:N with `dietary_preferences` (ElementCollection)
- 1:N with `allergies` (ElementCollection)
- 1:N with `disliked_ingredients` (ElementCollection)
- 1:N with `cuisine_preferences` (ElementCollection)

---

## Collection Tables (ElementCollection)

These are supporting tables that store Set/List collections for `nutritional_preferences`:

### 4. `dietary_preferences`
**Purpose:** Stores dietary preferences for each user

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to nutritional_preferences |
| preference | VARCHAR | NOT NULL | Dietary preference value (e.g., vegetarian, gluten-free) |

**Primary Key:** (user_id, preference)

---

### 5. `allergies`
**Purpose:** Stores food allergies and intolerances for each user

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to nutritional_preferences |
| allergen | VARCHAR | NOT NULL | Allergen name (e.g., peanuts, shellfish) |

**Primary Key:** (user_id, allergen)

---

### 6. `disliked_ingredients`
**Purpose:** Stores ingredients the user dislikes

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to nutritional_preferences |
| ingredient | VARCHAR | NOT NULL | Ingredient name (e.g., mushrooms, olives) |

**Primary Key:** (user_id, ingredient)

---

### 7. `cuisine_preferences`
**Purpose:** Stores cuisine preferences for each user

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to nutritional_preferences |
| cuisine | VARCHAR | NOT NULL | Cuisine type (e.g., Italian, Mexican, Asian) |

**Primary Key:** (user_id, cuisine)

---

## Activity & Health Tracking

### 8. `activity_entries`
**Purpose:** Records user physical activity

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Activity entry ID |
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to users table |
| activity_type | VARCHAR | - | Type of activity (e.g., running, cycling) |
| duration_minutes | INTEGER | - | Duration in minutes |
| calories_burned | INTEGER | - | Estimated calories burned |
| date | DATE | - | Date of activity |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Relationships:**
- N:1 with `users` (via user_id)

**Indexes:**
- user_id
- date

---

### 9. `weight_entries`
**Purpose:** Records user weight tracking history

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Weight entry ID |
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to users table |
| weight_kg | DOUBLE | - | Weight in kilograms |
| date | DATE | - | Date of measurement |
| notes | VARCHAR | - | Optional notes |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Relationships:**
- N:1 with `users` (via user_id)

**Indexes:**
- user_id
- date

---

## Security & Utilities

### 10. `password_reset_tokens`
**Purpose:** Stores password reset tokens for security

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Token ID |
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to users table |
| token | VARCHAR | UNIQUE, NOT NULL | Reset token |
| expires_at | TIMESTAMP | NOT NULL | Token expiration time |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

**Relationships:**
- N:1 with `users` (via user_id)

**Indexes:**
- token (UNIQUE)

---

### 11. `user_consents`
**Purpose:** Tracks user consent decisions (privacy, terms, etc.)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Consent record ID |
| user_id | BIGINT | UNIQUE, FOREIGN KEY | Reference to users table |
| privacy_policy | BOOLEAN | DEFAULT false | Privacy policy consent |
| terms_of_service | BOOLEAN | DEFAULT false | Terms of service consent |
| marketing | BOOLEAN | DEFAULT false | Marketing communications consent |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| updated_at | TIMESTAMP | DEFAULT NOW() | Last update timestamp |

**Relationships:**
- 1:1 with `users` (via user_id)

---

## AI & Caching

### 12. `ai_insight_cache`
**Purpose:** Caches AI-generated insights to reduce API calls

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Cache entry ID |
| user_id | BIGINT | FOREIGN KEY, NOT NULL | Reference to users table |
| insight_type | VARCHAR | NOT NULL | Type of insight (e.g., recommendation, analysis) |
| content | TEXT | - | Cached insight content |
| expires_at | TIMESTAMP | NOT NULL | Cache expiration time |
| created_at | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

**Relationships:**
- N:1 with `users` (via user_id)

**Indexes:**
- user_id
- expires_at

---

## Entity-Relationship Diagram (Text)

```
┌─────────────┐
│   users     │
├─────────────┤
│ id (PK)     │
│ username    │
│ email       │
│ password    │
└──────┬──────┘
       │
       ├─────────────────────────────┐
       │                             │
       ▼                             ▼
┌─────────────────┐    ┌──────────────────────────────┐
│   profiles      │    │ nutritional_preferences (1:1)│
├─────────────────┤    ├──────────────────────────────┤
│ user_id (FK)    │    │ user_id (PK, FK)             │
│ age             │    │ calorie_target               │
│ gender          │    │ protein_target               │
│ height_cm       │    │ carbs_target                 │
│ weight_kg       │    │ fats_target                  │
│ target_weight_kg│    │ breakfast_count              │
│ activity_level  │    │ lunch_count                  │
│ goal            │    │ dinner_count                 │
└─────────────────┘    │ snack_count                  │
                       │ breakfast_time               │
                       │ lunch_time                   │
                       │ dinner_time                  │
                       │ snack_time                   │
                       └──────────┬──────────────────┘
                                  │
                    ┌─────────────┼─────────────┬──────────────┐
                    │             │             │              │
                    ▼             ▼             ▼              ▼
          ┌──────────────┐ ┌────────┐ ┌─────────────────┐ ┌──────────────────┐
          │ dietary_pref │ │allergies│ │disliked_ingred. │ │cuisine_preferen. │
          └──────────────┘ └────────┘ └─────────────────┘ └──────────────────┘
       
       ├─────────────────────────────┐
       │                             │
       ▼                             ▼
┌──────────────────┐    ┌──────────────────┐
│ activity_entries │    │  weight_entries  │
├──────────────────┤    ├──────────────────┤
│ user_id (FK)     │    │ user_id (FK)     │
│ activity_type    │    │ weight_kg        │
│ duration_minutes │    │ date             │
│ calories_burned  │    │ notes            │
│ date             │    └──────────────────┘
└──────────────────┘
       
       ├─────────────────────────────┐
       │                             │
       ▼                             ▼
┌──────────────────────┐   ┌──────────────────┐
│ password_reset_token │   │  user_consents   │
├──────────────────────┤   ├──────────────────┤
│ user_id (FK)         │   │ user_id (FK)     │
│ token (UNIQUE)       │   │ privacy_policy   │
│ expires_at           │   │ terms_of_service │
└──────────────────────┘   │ marketing        │
                           └──────────────────┘
       
       ▼
┌──────────────────────┐
│ ai_insight_cache     │
├──────────────────────┤
│ user_id (FK)         │
│ insight_type         │
│ content              │
│ expires_at           │
└──────────────────────┘
```

---

## Data Types Summary

| Type | Usage | Examples |
|------|-------|----------|
| BIGSERIAL | Primary Keys | user.id, profiles.id |
| BIGINT | Foreign Keys | user_id references |
| VARCHAR | Text (variable) | username, email, gender |
| VARCHAR(N) | Text (max N chars) | gender (16), goal (32) |
| INTEGER | Whole numbers | age, duration_minutes |
| DOUBLE | Decimal numbers | weight_kg, height_cm |
| BOOLEAN | True/False | consent flags |
| TEXT | Large text | AI content |
| DATE | Date only | activity date |
| TIMESTAMP | Date + Time | created_at, updated_at |

---

## Constraints & Indexes

### Primary Keys (PK)
- All main tables have surrogate BIGSERIAL PK
- Collections use composite keys (user_id, value)

### Foreign Keys (FK)
- user_id references users(id)
- All with DELETE CASCADE for referential integrity

### Unique Constraints
- users: username, email
- nutritional_preferences: user_id (1:1 relationship)
- password_reset_tokens: token
- user_consents: user_id (1:1 relationship)

### Indexes
- user_id on all user-related tables (for quick lookups)
- date on activity_entries and weight_entries
- expires_at on password_reset_tokens and ai_insight_cache

---

## Cleanup History

**Date:** 2026-01-19

**Removed 21 unused tables:**
- user_meal_timings
- user_cuisine_preferences
- user_disliked_ingredients
- user_dietary_preferences
- user_allergies_intolerances
- profile_cuisine_preferences
- profile_disliked_ingredients
- recipe_dietary_tags
- recipe_allergens
- recipe_ingredients
- ingredient_allergens
- recipe_steps
- recipes
- ingredients
- meal_plan_meals
- meal_plans
- weekly_meal_plans
- weekly_meal_entries
- daily_meal_plans
- daily_meal_entries
- nutrition_preferences

**Result:** Database optimized from 33 tables → 12 tables

---

## Notes

- ✅ All tables auto-created/managed by Hibernate ORM
- ✅ Configuration: `spring.jpa.hibernate.ddl-auto: update`
- ✅ Timestamps: All tables have `created_at` and `updated_at` fields
- ✅ Relationships: All foreign keys use CASCADE for consistency
- ✅ Collections: ElementCollection pattern used for Set/List fields in nutritional_preferences
