# AGENTS.md

## Build & test

```bash
# Build everything (includes tests)
./gradlew build --no-daemon

# Build only the runnable server JAR (used by Docker)
./gradlew :app:api:buildFatJar --no-daemon

# Run tests for a single module
./gradlew :domain:user:test
./gradlew :data:postgres:test
```

## Module architecture

- `domain:*` — pure interfaces/models. Depends **only** on `:platform:core`. No DB/IO code.
- `data:*` — implementations of domain interfaces. Depends on `:platform:database` + relevant `:domain:*` modules.
- `app:api` — Ktor entrypoint (`Main.kt`), routes, controllers, DI wiring (`AppModules.kt`).
- `app:jobs` — background job schedulers (news feed, eBay marketplace).
- `platform:core` — shared types (`ApiResponse`, `Result`), config loaders (env-var based), error handling.
- `platform:auth` — JWT auth Ktor plugin (`"jwt-auth"`).
- `platform:database` — Flyway migrations, HikariCP `DataSource` factory, `BaseRepository`, and **test fixtures** (`PostgresTestContainer`).
- `platform:storage` / `platform:logging` / `platform:billing` — small infrastructure modules.

## Testing

- Tests use `kotlin.test` (`@Test`, `@BeforeTest`), **not** JUnit.
- Domain tests use **fakes** defined in the test source set (e.g. `FakeUserRepository`, `FakePasswordHasher`). No DB dependency.
- DB integration tests use `PostgresTestContainer` from `:platform:database` test fixtures. Import it as `com.vlatkogalev.platform.database.PostgresTestContainer` and use `PostgresTestContainer.dataSource`.
- Integration test cleanup (`@BeforeTest`) must delete rows in FK-safe order (children before parents).

## Database

- Flyway migrations live in `platform/database/src/main/resources/db/migration/`.
- Migrations run automatically at app startup (inside `appModule` in `AppModules.kt`), **not** via a Gradle task.
- `DataSource` is a Koin singleton; it also runs migrations eagerly during creation.

## Config & env

- All config is loaded from `System.getenv()` via `loadXxxConfig()` functions in `platform:core/config`.
- `.env` files are gitignored. `.env.hostinger.example` is the deployment reference.
- `app:api/Main.kt` reads `PORT` env var (default `8080`) for the Netty port.

## Key dependencies & version catalogs

- JDK 21 (enforced via `jvmToolchain(21)` in every module).
- Ktor 3.4 (Ktor version catalog as `ktorLibs` in `settings.gradle.kts`).
- Koin 4.2 for DI.
- Custom version catalog at `gradle/libs.versions.toml` (aliased `libs`).
- Two version catalogs coexist: `ktorLibs` and `libs`.

## CI & Docker

- CI (`ci.yml`): `./gradlew build --no-daemon` then Docker push to GHCR on `main`.
- Deploy (`deploy-hostinger-vps.yml`): triggers after CI on `main`, uses `hostinger/deploy-on-vps@v2` with `docker-compose.yml`.
- Dockerfile builds a **fat JAR** via `:app:api:buildFatJar`, merges `META-INF/services` files.

## Style & conventions

- Kotlin official code style.
- Gradle configuration cache and build cache are on, with high memory (`-Xmx12g` for Gradle, `-Xmx8g` for Kotlin daemon).
- Controllers register route extensions like `registerProtectedRoutes()` / `registerPublicRoutes()` called from `Routes.kt`.
