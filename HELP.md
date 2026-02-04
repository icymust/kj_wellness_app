
## Environment setup (.env)

Create a `.env` file in the project root (next to `docker-compose.yml`). Do NOT commit it.

Template (see `.env.example`):

```
GITHUB_CLIENT_ID=
GITHUB_CLIENT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# Frontend origin used for OAuth redirects (dev with docker-compose)
FRONTEND_ORIGIN=http://localhost:8080

# Mail (SMTP) — use Mailtrap.io sandbox credentials
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=

# JWT signing secret (>=32 secure characters)
JWT_SECRET=

# Groq API key (AI features)
GROQ_API_KEY=
```

Generate a secure secret (macOS / Linux):

```bash
openssl rand -base64 48 | tr -d '\n'
```

Put the result into `JWT_SECRET`. Rotate when moving to production.

## Groq API key (AI features)

To enable AI features (recipe generation and nutrition insights), create a Groq API key:

1. Go to Groq Console: https://console.groq.com/keys
2. Create a new API key.
3. Copy it into your root `.env` as:
   `GROQ_API_KEY=your_key_here`
4. Restart Docker so the backend picks it up.

## Obtaining OAuth credentials

GitHub (OAuth Apps):  https://github.com/settings/developers  → New OAuth App
- Homepage URL: `http://localhost:8080`
- Authorization callback URL: `http://localhost:5173/login/oauth2/code/github` (backend port)

Google Cloud Console: https://console.cloud.google.com/auth/clients/
- Create OAuth Client ID (Web application)
- Authorized JavaScript origins: `http://localhost:8080`
- Authorized redirect URIs: `http://localhost:5173/login/oauth2/code/google`

Copy Client ID / Client Secret values into `.env`.

## Mail setup (Mailtrap.io)

We use Mailtrap for local/dev email delivery (sandbox).

- Create an account/inbox: https://mailtrap.io/
- Open SMTP credentials for your inbox and copy:
  - Host (e.g. `smtp.mailtrap.io`)
  - Port (e.g. `2525`)
  - Username
  - Password
- Put them into the root `.env` as `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
- Restart with Docker to pass variables into backend.

## .env and docker-compose linkage

`docker-compose.yml` passes environment variables into the `app` service (backend). Example block:

```yaml
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/ndl
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: postgres
    SERVER_PORT: 5173
    GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:-}
    GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:-}
    GITHUB_CLIENT_ID: ${GITHUB_CLIENT_ID:-}
    GITHUB_CLIENT_SECRET: ${GITHUB_CLIENT_SECRET:-}
    FRONTEND_ORIGIN: ${FRONTEND_ORIGIN:-}
    MAIL_HOST: ${MAIL_HOST:-}
    MAIL_PORT: ${MAIL_PORT:-}
    MAIL_USERNAME: ${MAIL_USERNAME:-}
    MAIL_PASSWORD: ${MAIL_PASSWORD:-}
    JWT_SECRET: ${JWT_SECRET:-changeme-dev}
```

The backend reads the secret via `app.jwt.secret` (`application.yml`). For dynamic usage ensure it references `${JWT_SECRET}` (already configured).

## Frontend base URL property

In `application.yml` you can define:

```yaml
app:
  frontend:
    base-url: ${FRONTEND_ORIGIN:http://localhost:8080}
```

Useful for generating links (redirects, CORS). Document in README when deploying.

Note: The separate `frontend/.env` file was removed; configure `FRONTEND_ORIGIN` via the root `.env`.

## pgAdmin: login & database creation

Service available at: http://localhost:5050

Credentials (from docker-compose):
- Email: `admin@example.com`
- Password: `admin`

Steps:
1. Open http://localhost:5050 and log in.
2. Click "Add New Server".
3. General → Name: `ndl-local`.
4. Connection:
   - Host: `db` (Docker service name for Postgres)
   - Port: `5432`
   - Maintenance DB: `postgres`
   - Username: `postgres`
   - Password: `postgres`
   - Save Password: On
5. Save. Server appears in the list.
6. If database `ndl` is missing: expand server → Databases → Create Database → Name: `ndl` → Owner: `postgres` → Save.

Verification:
Expand `ndl` → Schemas → public → Tables (created automatically by JPA on app start).

## Fast development cycle

Run with Docker:
```bash
docker compose up --build
```

Frontend: http://localhost:8080
Backend API: http://localhost:5173

Stop:
```bash
docker compose down
```

## Health / Export

Use endpoint `/export/health` (requires auth) to download full data. Frontend button "Download health data" saves `export.json` containing profile, weights, activities, consent.

---
OAuth registration links:
https://github.com/settings/developers
https://console.cloud.google.com/auth/clients/
