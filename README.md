# ktor-backend-template

Modular Ktor backend template (Kotlin/JVM) with clean separation between app layers and reusable platform modules.

## Tech stack
- Kotlin 2.3
- Ktor 3.4
- Koin 4.2
- JWT auth
- Flyway + HikariCP
- H2 (default local DB) / PostgreSQL
- AWS S3 SDK (storage module)
- Gradle multi-module build

## Project structure
- `app:api` - runnable Ktor app (entry point, routes, DI wiring)
- `app:domain` - domain models and service contracts
- `app:data` - service implementations and repositories
- `app:jobs` - background/business job orchestration
- `platform:core` - shared core utilities/config/result types
- `platform:auth` - JWT auth plugin + token provider
- `platform:database` - DB integration/migrations dependencies
- `platform:storage` - S3-backed storage service
- `platform:logging` - call logging setup
- `platform:billing` - billing domain placeholder module

## Requirements
- JDK 21
- Gradle Wrapper (included)

## Hostinger VPS deployment (template-ready)
This template includes:
- `.github/workflows/deploy-hostinger-vps.yml`
- `docker-compose.yml`
- `.env.example`

### 1) One-time setup in each new repository
In GitHub repository settings (`Settings -> Secrets and variables -> Actions`), add:

- **Secret** `HOSTINGER_API_KEY`: Hostinger API key
- **Variable** `HOSTINGER_VM_ID`: Hostinger VPS ID
- **Variable** `HOSTINGER_PROJECT_NAME`: optional project name shown in Hostinger (can be same as repo name)

Add DB/runtime values as **Secrets**:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

### 2) Private repository requirement (Hostinger Docker Manager)
For private repos, configure SSH Deploy Key on Hostinger VPS and add it to the GitHub repo deploy keys, per Hostinger guide:
- [Deploy from private GitHub repo on Hostinger Docker Manager](https://www.hostinger.com/support/how-to-deploy-from-private-github-repository-on-hostinger-docker-manager/)

### 3) Trigger deployment
Push to `main` (or run the workflow manually). The workflow uses:
- `hostinger/deploy-on-vps@v2`
- `docker-compose.yml`

### 4) Minimal changes you usually need for a new project
- Optionally adjust `APP_PORT` in `docker-compose.yml`
- Confirm DB values/secrets for target environment
- If needed, adapt service/container names in `docker-compose.yml`
