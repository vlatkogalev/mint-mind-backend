# AGENTS.md — MintMind Backend

## Commands

```bash
./gradlew build --no-daemon                 # Compile + test (what CI runs)
./gradlew :app:api:run                      # Local dev server on PORT or :8080
./gradlew :app:api:buildFatJar              # Shadow JAR for Docker image

# Run a single module's tests
./gradlew :domain:user:test                 # Auth domain (fast, in-memory fakes)
./gradlew :domain:coin:test                 # Coin domain (fast, in-memory fakes)
./gradlew :app:api:test                     # DTO validation tests
./gradlew :data:postgres:test               # Integration tests (needs Docker → Testcontainers)
```

`./gradlew build` runs tests automatically. Integration tests in `:data:postgres` require Docker for Testcontainers; the fixture is `PostgresTestContainer` in `platform/database` testFixtures.

No linting or static analysis is configured (no ktlint, detekt, Jacoco).

## Architecture

### Domain purity (`domain:*` modules)

Domain modules depend **only** on `platform:core`. No database queries, no HTTP, no I/O. They define interfaces (repositories, services, models). Implementations live in `data:*` (Postgres, S3, Resend, eBay, Numista).

### `Result<T>` — never throw

Services return `Result<T>` (`Success` / `Failure`). Use `resultOf { }` to wrap exceptions. Controllers map `Failure` to HTTP status codes.

All API responses are wrapped in `ApiResponse<T>`: `{ success, data, error, timestampMillis }`. This is enforced by `configureCore()`.

### Exposed R2DBC, not JDBC

All queries use `suspendTransaction` / `dbQuery` with `R2dbcDatabase`. The `r2dbcUrl` is derived by `"jdbc:" → "r2dbc:"` string replace on the JDBC URL. **Always use the `R2dbcDatabase` singleton, never create a JDBC connection for queries.**

### Database startup flow

Flyway migrations run **before** the server starts, inside a `createdAtStart` Koin singleton (`AppModules.kt:71`):
1. Open temporary JDBC `HikariDataSource`
2. Run Flyway migrations (SQL files in `platform/database/src/main/resources/db/migration/`)
3. Close JDBC pool
4. Open `R2dbcDatabase` pool

Migrations are idempotent (`IF NOT EXISTS`, `DROP TRIGGER IF EXISTS`).

### Dual HTTP clients

- **Server:** Ktor Netty (inbound API)
- **Client:** Ktor CIO (outbound to Numista catalog, eBay APIs)

## Auth

- JWT access tokens: Auth0 `java-jwt`, HMAC-256. Configured via `AUTH_SECRET` env var.
- Refresh tokens: `userId:randomUUID` string (not a JWT), bcrypt-hashed before storage.
- `AUTH_SKIP_VERIFICATION=true` bypasses email verification in local/dev.

## Config

- Config loaded from `System.getenv()` (see `.env.example`). No `application.conf`, only `application.yaml` for Ktor static settings.
- Two Gradle version catalogs: `libs` (project) and `ktorLibs` (from `io.ktor:ktor-version-catalog:3.4.3`).
- Swagger docs served at `/docs` from a manually-maintained `documentation.yaml`.
- CORS is intentionally wide open (`anyHost()`).

## CI/CD

- `.github/workflows/ci.yml` — runs on push to `main` and PRs. Builds, tests, pushes Docker image to GHCR on `main`.
- `.github/workflows/deploy-hostinger-vps.yml` — deploys GHCR image to Hostinger VPS on CI success.
