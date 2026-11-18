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

## Comparer des instruments en base 100

L’application expose une comparaison des cours historiques ajustés sur une période inclusive:

```shell
curl "http://localhost:8080/api/comparisons/base-100?symbolIds=1,2&from=2026-01-01&to=2026-10-31"
```

Paramètres:

- `symbolIds`: identifiants internes des instruments, séparés par des virgules (maximum 50).
- `from`: date de début au format ISO `YYYY-MM-DD`.
- `to`: date de fin au format ISO `YYYY-MM-DD`, supérieure ou égale à `from`.

Pour chaque instrument, le premier cours disponible dans la période devient la baseline à 100. Les autres valeurs sont calculées avec `adjusted_close`:

```text
base100 = 100 × coursCourant / coursDeBaseline
```

Les séries ont une précision de six décimales. Les instruments sans cours ou avec une baseline invalide sont retournés dans `ignoredSymbolIds`. La base 100 ne stocke aucune valeur normalisée; elle est calculée à la lecture. Il n’y a aucune interpolation des jours de cotation ou conversion de devise.

## Dashboard base 100

Une fois l’application démarrée sur `localhost:8080`, ouvrir:

```text
http://localhost:8080/dashboard/
```

Le dashboard permet de:

- rechercher et sélectionner jusqu’à 50 instruments;
- choisir une période inclusive;
- afficher une courbe base 100 par instrument;
- consulter les tooltips, les instruments ignorés et un tableau récapitulatif;
- activer ou masquer chaque série depuis la légende.

Chaque instrument est normalisé indépendamment à 100 à son premier cours disponible dans la période. Les devises ne sont pas converties et les jours de cotation manquants ne sont pas interpolés.

## Stop PostgreSQL

Stop the service while preserving its data:

```shell
docker compose down
```

To also delete the persisted database data:

```shell
docker compose down -v
```
