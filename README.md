# MintMind Backend v2

Kotlin/Ktor backend for the MintMind coin collection app — AI-powered coin recognition storage, catalog enrichment, news aggregation, eBay marketplace pricing, and S3 image management.

## Tech Stack

| Concern | Technology |
|---------|------------|
| Language | Kotlin 2.3, JDK 21 |
| HTTP Framework | Ktor 3.4 (Netty) |
| DI | Koin 4.2 |
| Database | PostgreSQL 15 via Exposed 1.3 (R2DBC) |
| Migrations | Flyway 10 |
| Connection Pool | HikariCP 6 |
| Auth | JWT (HMAC-256) + bcrypt |
| Email | Resend Java SDK |
| Object Storage | AWS S3 SDK v2 (presigned URLs) |
| Serialization | kotlinx.serialization |
| Build | Gradle 9.5, Kotlin DSL |
| Containerization | Docker (multi-stage), Docker Compose |
| CI/CD | GitHub Actions → GHCR → Hostinger VPS |

## Features

- **Auth** — Anonymous sessions, email/password registration, JWT access + refresh tokens, email verification, password reset, session merging
- **Coins & Sets** — Save AI-recognized coins with full metadata and catalogue numbers, organize into named sets, filter/sort/paginate
- **Catalog Enrichment** — Automatic matching against Numista coin catalog with a scoring algorithm, manual re-enrichment endpoint
- **News Feed** — Background RSS fetcher from CoinWeek with HTML sanitization, category filtering, deduplication
- **eBay Marketplace** — Background job fetching live high-value certified coin listings, on-demand coin pricing via two-pass eBay Browse API query
- **Billing** — RevenueCat webhook handler mirroring subscription state (Free/Pro/Enterprise)
- **S3 Storage** — Presigned upload/download URL generation scoped to authenticated users

## Project Structure

```
├── app/
│   ├── api/          # Ktor entry point, routing, controllers, DI wiring
│   └── jobs/         # Background schedulers (RSS news, eBay marketplace)
├── domain/
│   ├── billing/      # Subscription models, service, repository interface
│   ├── coin/         # Coin, set, catalog models, services, enrichment logic
│   ├── marketplace/  # Marketplace listing model and repository interface
│   ├── news/         # News article model and repository interface
│   ├── pricing/      # Active listing, price range, pricing service interface
│   └── user/         # User account, auth service, repository interface
├── data/
│   ├── ebay/         # eBay OAuth token provider, marketplace fetcher, pricing service
│   ├── email/        # Resend email sender implementations
│   ├── numista/      # Numista catalog provider
│   ├── postgres/     # PostgreSQL repository implementations (Exposed R2DBC)
│   └── s3/           # S3 presigned URL generation
└── platform/
    ├── auth/         # Ktor JWT plugin, token provider, password hasher
    ├── core/         # Result<T>, ApiResponse<T>, TimeProvider, config loaders
    ├── database/     # HikariCP, Flyway, Exposed setup, dbQuery helper
    └── logging/      # Call logging and structured logger
```

### Architecture Rules

- `domain:*` modules contain only interfaces, models, and service implementations. They depend **only** on `platform:core`. No DB code, no HTTP code, no I/O.
- `data:*` modules implement domain interfaces using DB, HTTP clients, and external SDKs.
- `app:api` wires everything via Koin and exposes HTTP endpoints via Ktor controllers.
- Services return `Result<T>` (never throw). Controllers map failures to HTTP status codes.

## Requirements

- JDK 21
- PostgreSQL 15
- Gradle Wrapper (included)

## Quick Start

```bash
# 1. Clone and set up environment
cp .env.example .env
# Edit .env with your values

# 2. Create the database
createdb mint_mind

# 3. Run
./gradlew :app:api:run

# API available at http://localhost:8080
# Swagger docs at http://localhost:8080/docs
```

### Required Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL (e.g. `jdbc:postgresql://localhost:5432/mint_mind`) |
| `DB_USER` | Database user |
| `DB_PASSWORD` | Database password |
| `AUTH_SECRET` | HMAC-256 secret for JWT signing |
| `RESEND_API_KEY` | Resend API key for transactional email |
| `EMAIL_FROM` | From address for emails |
| `APP_BASE_URL` | Base URL for email links |

See `.env.example` for all variables including optional eBay, Numista, S3, and RevenueCat config.

## API Endpoints

All responses wrapped in `{ success, data, error, timestampMillis }`.

### Auth (`/auth`) — mixed public/protected
- `POST /auth/anonymous` — bootstrap anonymous session
- `POST /auth/register` — create registered account
- `POST /auth/login` — login with email/password
- `POST /auth/refresh` — rotate refresh token
- `GET /auth/verify-email` — verify email (returns HTML)
- `POST /auth/resend-verification` — resend verification email
- `POST /auth/password-reset/request` — request reset email
- `POST /auth/password-reset/confirm` — confirm with token
- `POST /auth/upgrade-account` — anonymous → registered (optional auth)
- `GET /auth/me` — get profile (protected)
- `PATCH /auth/me` — update profile (protected)
- `DELETE /auth/me` — delete account (protected)

### Coins (`/coins`) — protected
- `POST /coins` — save recognized coin
- `GET /coins` — list coins (filterable, paginated)
- `GET /coins/stats` — collection statistics
- `GET /coins/{id}` — get coin
- `DELETE /coins/{id}` — delete coin
- `PATCH /coins/{id}/notes` — update notes
- `GET /coins/{id}/images` — presigned image URLs
- `POST /coins/{id}/enrich` — manual catalog enrichment
- `GET /coins/{id}/pricing` — eBay pricing estimate

### Sets (`/sets`) — protected
- `POST /sets` — create set
- `GET /sets` — list sets
- `GET /sets/{id}` — get set
- `PATCH /sets/{id}` — update set
- `DELETE /sets/{id}` — delete set
- `POST /sets/{id}/coins` — add coins to set
- `DELETE /sets/{id}/coins` — remove coins from set

### News (`/news`) — public
- `GET /news` — list articles (paginated)
- `GET /news/{id}` — get article

### Marketplace (`/marketplace`) — public
- `GET /marketplace/listings` — list eBay listings (paginated)

### Storage (`/storage`) — protected
- `POST /storage/upload-urls` — presigned S3 upload URLs
- `POST /storage/download-urls` — presigned S3 download URLs

### Webhooks (`/webhooks`) — secret auth
- `POST /webhooks/revenuecat` — RevenueCat subscription events

## Testing

```bash
./gradlew test                    # all tests
./gradlew :domain:user:test       # auth domain unit tests
```

- Domain tests use in-memory fakes — no database required
- Database integration tests use Testcontainers (PostgreSQL)

## Deployment

CI/CD via GitHub Actions:
1. `ci.yml` — builds and runs tests on push to `main` and PRs; on `main`, also builds and pushes Docker image to GHCR
2. `deploy-hostinger-vps.yml` — deploys to Hostinger VPS using `docker-compose.yml`

See `.github/workflows/` for details. Required GitHub secrets: `HOSTINGER_API_KEY`, `HOSTINGER_VM_ID`, plus all DB and runtime environment variables.
