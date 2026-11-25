# coupon-service

A service for managing discount coupons. Spring Boot, PostgreSQL, built with Gradle.

## Requirements

- JDK 25 installed locally (the Gradle toolchain finds it; it is not downloaded automatically)
- Docker: local database (`compose.yaml`) and integration tests (Testcontainers)

Gradle does not need to be installed — the project uses the Gradle Wrapper (`./gradlew`, `gradlew.bat` on Windows).

## Running locally

1. Start the PostgreSQL database:

   ```bash
   docker compose up -d
   ```

2. Start the application with the `local` profile (it contains the database credentials from `compose.yaml`):

   ```bash
   SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
   ```

The database schema is created by Flyway on application startup (`src/main/resources/db/migration`).

The application listens on `http://localhost:8080`.

The database with its data can be removed and recreated (e.g. after changing an existing migration):

```bash
docker compose down -v && docker compose up -d
```

### Running without the `local` profile

Without the `local` profile the database credentials must be provided as environment variables:

| Variable      | Example                                   |
|---------------|-------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/coupon` |
| `DB_USERNAME` | `coupon`                                  |
| `DB_PASSWORD` | `coupon`                                  |

Running the built JAR file:

```bash
./gradlew bootJar
DB_URL=jdbc:postgresql://localhost:5432/coupon DB_USERNAME=coupon DB_PASSWORD=coupon \
  java -jar build/libs/coupon-service-0.0.1-SNAPSHOT.jar
```

### Docker

The application together with the database can be run with Docker Compose. The `app` service has the `app` profile, so
a plain `docker compose up -d` still starts only the database (for working with `bootRun`):

```bash
docker compose --profile app up -d --build    # database + application on http://localhost:8080
docker compose --profile app down             # stop (database data stays in the volume)
```

In Compose the application container runs with a read-only file system (only `/tmp` is writable), without additional
kernel capabilities (`cap_drop: ALL`) and with privilege escalation blocked (`no-new-privileges`).

Building the image alone (tests are not run during the image build — CI runs them):

```bash
docker build -t coupon-service .
```

Running against the database from `compose.yaml` (running on the host):

```bash
docker run --rm --network host \
  -e DB_URL=jdbc:postgresql://localhost:5432/coupon -e DB_USERNAME=coupon -e DB_PASSWORD=coupon \
  coupon-service
```

Configuration is provided via environment variables (see [Configuration](#configuration)). JVM settings can be
overridden with the `JAVA_TOOL_OPTIONS` variable (default `-XX:MaxRAMPercentage=75.0`, i.e. heap up to 75% of the
container memory limit).

Image build (`Dockerfile`):

- two stages: build on a JDK (Gradle), run on a JRE only — build tools do not end up in the image,
- Eclipse Temurin 25 base images pinned to version and digest: JDK on Ubuntu 24.04 for building, JRE on Alpine Linux
  for running (smaller image, fewer system packages),
- the layer with Gradle and dependencies is rebuilt only when Gradle files change, not on every code change,
- the JAR is extracted into [Spring Boot layers](https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html)
  (dependencies separate from application code) — after a code change only the application layer changes (approx.
  100 kB),
- the application runs as a non-root user (`app`, UID 10001), and its files are owned by root (read-only),
- `.dockerignore` lets only the files needed to build the application into the build context.

## Configuration

Application settings are in `src/main/resources/application.yaml`. Every value can be overridden with an environment
variable (dashes are dropped, dots replaced with `_`, e.g. `COUPONSERVICE_FREEIPAPI_READTIMEOUT=3s`) or with a
`--key=value` argument. All `coupon-service.*` keys are required — if any of them is missing, the application fails to
start.

### Free IP API (`coupon-service.free-ip-api`)

The user's country is determined from the IP address using [Free IP API](https://freeipapi.com/). The free plan allows
approx. 60 requests per minute.

| Key                 | Default                      | Description                                        |
|---------------------|------------------------------|----------------------------------------------------|
| `base-url`          | `https://free.freeipapi.com` | API address                                        |
| `connect-timeout`   | `5s`                         | connection timeout                                 |
| `read-timeout`      | `5s`                         | response read timeout                              |
| `retry.max-retries` | `3`                          | number of retries after a 5xx or connection error  |
| `retry.delay`       | `200ms`                      | delay before the first retry                       |
| `retry.multiplier`  | `2`                          | delay multiplier for subsequent retries            |
| `retry.jitter`      | `100ms`                      | random delay deviation                             |
| `retry.max-delay`   | `2s`                         | maximum delay between retries                      |

4xx errors (e.g. 429 after exceeding the limit) are not retried. When the API remains unavailable, the coupon
redemption request ends with a `503` response with a `Retry-After` header.

### Cache (`coupon-service.cache`)

| Cache               | `maximum-size` | `expire-after-write` | Content                                                          |
|---------------------|----------------|----------------------|------------------------------------------------------------------|
| `ip-countries`      | `50000`        | `12h`                | country determined for an IP address — limits Free IP API calls  |
| `exhausted-coupons` | `10000`        | `30d`                | exhausted coupons — rejected without a database query            |

The cache is local to each application instance (Caffeine).

### Database

| Key                                                      | Default | Description                                                                          |
|----------------------------------------------------------|---------|--------------------------------------------------------------------------------------|
| `spring.jpa.properties.jakarta.persistence.lock.timeout` | `2000`  | maximum time (ms) to wait for the coupon lock during redemption; `503` when exceeded |

Database credentials — see [Running without the `local` profile](#running-without-the-local-profile).

### Tracing

Request tracing is provided by Micrometer Tracing with OpenTelemetry (`spring-boot-starter-opentelemetry`):

- every HTTP request gets a `traceId` ([W3C Trace Context](https://www.w3.org/TR/trace-context/) standard); if the
  request contains a `traceparent` header, the application continues the given trace,
- `traceId` and `spanId` are added to logs (`[traceId-spanId]`) and returned in error responses as `correlationId`
  (see [API](#api)),
- spans are not exported anywhere until the OpenTelemetry collector address is set
  (`management.opentelemetry.tracing.export.otlp.endpoint`, e.g. `http://localhost:4318/v1/traces`); by default 10% of
  traces are sampled (`management.tracing.sampling.probability`),
- OTLP metrics export is disabled (`management.otlp.metrics.export.enabled: false`), because there is no collector yet.

Free IP API calls are not traced (external API, outside the organization).

### Health checks

Spring Boot Actuator exposes application health endpoints (e.g. for Kubernetes probes):

- liveness: http://localhost:8080/actuator/health/liveness — only the state of the application itself (a database
  outage does not cause a restart),
- readiness: http://localhost:8080/actuator/health/readiness — application state and database connection (without the
  database the instance does not accept traffic).

The healthcheck of the `app` service in `compose.yaml` uses the readiness endpoint.

## API

OpenAPI documentation (with the application running):

- Swagger UI: http://localhost:8080/swagger-ui.html
- JSON specification: http://localhost:8080/v3/api-docs

Sample requests for the IntelliJ IDEA HTTP client are in the `http/` directory (`local` environment).

Errors are returned in the [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) format (`application/problem+json`), with
additional fields `timestamp` (time the error occurred) and `correlationId` (the request's `traceId` — helps find the
related log entries):

```json
{
  "title": "Coupon not found",
  "status": 404,
  "detail": "Coupon with code WINTER2026 not found",
  "timestamp": "2026-11-23T11:15:30.123Z",
  "correlationId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

## Tests

| Command                      | Scope                                                                                                                           |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `./gradlew test`             | unit tests (without `*IT` classes), do not require Docker                                                                       |
| `./gradlew integrationTest`  | integration tests (`*IT` classes), PostgreSQL started in Testcontainers, Free IP API replaced by [WireMock](https://wiremock.org/) |
| `./gradlew check`            | unit tests, integration tests, formatting check and code coverage threshold                                                     |
| `./gradlew jacocoTestReport` | unit and integration tests + code coverage report ([JaCoCo](https://www.jacoco.org/jacoco/))                                    |

Tests are written in Groovy ([Spock](https://spockframework.org/)).

Integration tests extend `IntegrationTestSpecification`, which activates the `test` profile. Test-only settings (e.g.
shorter Free IP API retry delays) are in `src/test/resources/application-test.yaml`.

The coverage report (combined for both kinds of tests) goes to `build/reports/jacoco/test/html/index.html` (XML version:
`build/reports/jacoco/test/jacocoTestReport.xml`).

The minimum line and branch coverage is 80% — checked by `./gradlew jacocoTestCoverageVerification` (also run as part
of `./gradlew check`/`build` and in CI); below the threshold the build fails.

## Building

Full build: compilation, all tests (including integration tests — requires Docker), formatting check and code coverage
threshold:

```bash
./gradlew build
```

The executable JAR file goes to `build/libs/`.

Versions of Gradle plugins, libraries not managed by Spring Boot and tools (JaCoCo, palantir-java-format) are defined in
`gradle.properties`.

## Code formatting

Formatting is enforced by [Spotless](https://github.com/diffplug/spotless):

- Java code — [palantir-java-format](https://github.com/palantir/palantir-java-format),
- `*.gradle` scripts — Eclipse Groovy formatter (greclipse), 4-space indentation,
- Groovy (Spock) tests — only import order (static first), semicolon removal, trailing whitespace, space indentation
  and a newline at the end of the file. The full formatter (greclipse) is not used because it breaks the alignment of
  `where:` tables. Test code layout is formatted in IntelliJ IDEA (*Reformat Code* aligns Spock tables); import and
  indentation settings for the IDE are in `.editorconfig`.

Formatting check (also run as part of `./gradlew check`/`build` and in CI):

```bash
./gradlew spotlessCheck
```

Automatic formatting fix:

```bash
./gradlew spotlessApply
```

In IntelliJ IDEA it is worth installing the **palantir-java-format** plugin, so that IDE formatting matches CI.

## CI

The GitHub Actions workflow (`.github/workflows/build.yml`) runs on every push to `main` and on pull requests to
`main`. Everything runs as a single `./gradlew build jacocoTestReport --continue` call: formatting check, unit and
integration tests, 80% coverage threshold check, artifact build and coverage report. Thanks to `--continue`, independent
tasks run even if another one fails, so a single run shows all problems at once (e.g. formatting errors and failing
tests).

The coverage report is available as the `coverage-report` artifact (when tests pass). If the build fails, test reports
are available as the `test-reports` artifact.
