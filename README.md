# FinSight

Minimal Spring Boot application built with Java 25 and Maven 3.10.0.

## Requirements

- Java 25
- Docker with Docker Compose

Maven is provided by the Maven Wrapper.

## Start PostgreSQL

Copy `.env.example` to `.env` and set a local database password. Docker Compose
loads `.env` automatically:

```shell
cp .env.example .env
docker compose up -d
```

The database is available on `localhost:5432`. The Compose service stores data
in a named Docker volume.

## Run the application

The Spring Boot process reads its connection settings from environment variables.
Set the same credentials as in `.env` before starting the application:

```shell
export DB_USERNAME=application
export DB_PASSWORD=change-me
./mvnw spring-boot:run
```

`DB_URL` is optional and defaults to
`jdbc:postgresql://localhost:5432/applicationdb`. Flyway applies pending
migrations at startup.

## Test

```shell
./mvnw clean verify
```

Integration tests use Testcontainers to start an isolated PostgreSQL 17
container, apply Flyway migrations, and stop the container after the tests.
Docker must be available; no local PostgreSQL instance or `.env` file is needed.

## Stop PostgreSQL

Stop the service while preserving its data:

```shell
docker compose down
```

To also delete the persisted database data:

```shell
docker compose down -v
```
