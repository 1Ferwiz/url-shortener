# URL Shortener API

[![CI](https://github.com/1Ferwiz/url-shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/1Ferwiz/url-shortener/actions/workflows/ci.yml)

A simple REST API for shortening URLs, built with Spring Boot. Demonstrates database persistence, caching, validation, and layered project organization following SOLID principles.

## Tech Stack

- Java 21
- Spring Boot 4
- PostgreSQL (persistent storage)
- Redis (caching — Cache-Aside pattern)
- Maven
- Lombok
- Docker / Docker Compose

## Architecture

The project follows a standard layered architecture:
controller/   → REST endpoints (HTTP layer)
service/      → Business logic (interface + implementation)
repository/   → Spring Data JPA data access
entity/       → JPA entities
dto/          → Request/response objects
exception/    → Custom exceptions + global exception handler
config/       → Configuration (Redis setup)
util/         → Helper classes (short code generator)

Key design decisions:
- **DTO pattern** — API contracts are fully decoupled from database entities.
- **Dependency Inversion** — Controller depends on the `UrlService` interface, not its implementation.
- **Repository pattern** — Spring Data JPA abstracts all database access.
- **Cache-Aside pattern** — Redis is checked first on reads; Postgres is only queried on a cache miss, then the result is written back to Redis.
- **Centralized exception handling** — `@RestControllerAdvice` converts exceptions into consistent, structured HTTP error responses.

## Prerequisites

- Java 21 (JDK)
- Docker Desktop (or Docker Engine + Compose)
- IntelliJ IDEA (or any IDE) — Maven is used via the bundled wrapper (`mvnw`), no separate Maven install required

## Running the project

1. **Start PostgreSQL and Redis:**
```bash
   docker compose up -d
```
This starts:
- PostgreSQL on `localhost:5433` (mapped from container port 5432)
- Redis on `localhost:6379`

2. **Run the application** (via IntelliJ's Run button, or):
```bash
   ./mvnw spring-boot:run
```
The API will be available at `http://localhost:8080`.


## CI/CD

Every push and pull request to `main` triggers a GitHub Actions pipeline (`.github/workflows/ci.yml`) that:

1. Spins up ephemeral PostgreSQL and Redis containers
2. Runs the full test suite against them
3. Builds a Docker image from the multi-stage `Dockerfile`
4. On `main` only: pushes the image to GitHub Container Registry (GHCR), tagged with both the commit SHA and `latest`

This confirms the application builds, passes its tests, and produces a working container image on a clean environment for every change — proving it's deployable, even though this project isn't hosted anywhere yet.

## API Endpoints

### Create a short URL
POST /api/urls
Content-Type: application/json
{
"url": "https://example.com/some/very/long/path"
}
**Response — `201 Created`**
```json
{
  "id": 1,
  "shortCode": "abc123",
  "url": "https://example.com/some/very/long/path"
}
```

### Retrieve the original URL
GET /api/urls/{shortCode}
**Response — `200 OK`**
```json
{
  "id": 1,
  "shortCode": "abc123",
  "url": "https://example.com/some/very/long/path"
}
```
Returns `404 Not Found` if the short code doesn't exist. Checks Redis first; falls back to PostgreSQL on a cache miss and repopulates Redis (24-hour TTL).

### List all URLs
GET /api/urls
**Response — `200 OK`** — array of all stored URLs.

## Validation

`POST /api/urls` validates that `url` is present and a syntactically valid URL. Invalid input returns `400 Bad Request` with details on which field(s) failed.

- **No URL deduplication** — each `POST /api/urls` call always creates a new short code, even for a previously-shortened URL. This is intentional: distinct short codes for the same destination allow independent tracking/expiry per link, rather than treating identical URLs as the same resource.