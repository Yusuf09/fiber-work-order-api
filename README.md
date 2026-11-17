# Fiber Work Order API

A Spring Boot REST API for managing fiber-optic installation jobs: dispatching work orders to technicians, recording optical signal readings on site, and only allowing a job to close when every reading is within spec.

I built it around the workflow I use as a fiber technician, where every job has a status, an assigned tech, and test results that decide whether the install is done.

![CI](https://github.com/Yusuf09/fiber-work-order-api/actions/workflows/ci.yml/badge.svg)

## Tech stack

- Java 21, Spring Boot 4 (Spring Web MVC, Spring Data JPA, Bean Validation, Actuator)
- Hibernate with H2 (default, in-memory) or PostgreSQL (`postgres` profile)
- JUnit 5, Mockito, AssertJ, MockMvc, `@DataJpaTest`
- Docker multi-stage build, Docker Compose, GitHub Actions CI

## Features

- **Work order lifecycle** `OPEN → ASSIGNED → IN_PROGRESS → COMPLETED`, with `CANCELLED` allowed before completion. Invalid transitions are rejected with `409 Conflict`.
- **Business rules in the domain model**: inactive technicians can't be assigned, readings can only be logged while a job is in progress, and a job can't be completed without at least one reading or with any reading outside **-27 to -8 dBm**.
- **Search with filtering and pagination** by status and technician.
- **Summary report** with counts for every status.
- **Consistent error responses** using RFC 9457 `ProblemDetail`, including per-field validation errors.
- **Optimistic locking** (`@Version`) so two people updating the same work order can't silently overwrite each other.
- **Testable time**: services use an injected `Clock`, so tests run against a fixed instant.

## Running it

Requires JDK 21.

```bash
./mvnw spring-boot:run          # H2 in-memory database on http://localhost:8080
./mvnw verify                   # runs the full test suite
```

With PostgreSQL in Docker:

```bash
docker compose up --build       # starts PostgreSQL and the API
```

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/technicians` | Create a technician |
| `GET` | `/api/technicians` / `/{id}` | List or get technicians |
| `POST` | `/api/technicians/{id}/deactivate` | Deactivate a technician |
| `POST` | `/api/work-orders` | Create a work order |
| `GET` | `/api/work-orders?status=&technicianId=&page=&size=` | Search work orders (paged) |
| `GET` | `/api/work-orders/{id}` | Get a work order with its readings |
| `POST` | `/api/work-orders/{id}/assign` | Assign to a technician `{"technicianId": 1}` |
| `POST` | `/api/work-orders/{id}/start` | Mark the job in progress |
| `POST` | `/api/work-orders/{id}/readings` | Record a reading `{"location": "ONT", "receivedPowerDbm": -19.4}` |
| `POST` | `/api/work-orders/{id}/complete` | Complete (all readings must pass) |
| `POST` | `/api/work-orders/{id}/cancel` | Cancel |
| `GET` | `/api/work-orders/summary` | Counts by status |

Example session:

```bash
curl -X POST localhost:8080/api/technicians -H 'Content-Type: application/json' \
  -d '{"name":"Sam Tech","email":"sam@example.com"}'

curl -X POST localhost:8080/api/work-orders -H 'Content-Type: application/json' \
  -d '{"customerName":"Jane Doe","address":"123 Main St, Minneapolis, MN","serviceType":"RESIDENTIAL","scheduledDate":"2026-12-01"}'

curl -X POST localhost:8080/api/work-orders/1/assign -H 'Content-Type: application/json' -d '{"technicianId":1}'
curl -X POST localhost:8080/api/work-orders/1/start
curl -X POST localhost:8080/api/work-orders/1/readings -H 'Content-Type: application/json' -d '{"location":"ONT","receivedPowerDbm":-31.0}'
curl -X POST localhost:8080/api/work-orders/1/complete
# 409: {"title":"Invalid state","detail":"All signal readings must be between -27.0 and -8.0 dBm to complete", ...}
```

## Project structure

```
src/main/java/com/yusufabdiqadir/fiberworkorders
├── common/        exception types, global error handler, Clock config
├── technician/    entity, repository, service, controller, DTOs
└── workorder/     WorkOrder aggregate, SignalReading, status rules, search query, service, controller, DTOs
```

## Tests

22 tests across four levels:

- `WorkOrderTest`: pure unit tests of the lifecycle and reading rules
- `WorkOrderServiceTest`: service logic with Mockito mocks and a fixed `Clock`
- `WorkOrderRepositoryTest`: JPQL search and aggregate queries against H2 (`@DataJpaTest`)
- `WorkOrderApiIntegrationTest`: full HTTP flows through MockMvc, including 400/404/409 error cases
