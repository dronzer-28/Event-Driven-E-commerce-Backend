# Event-Driven E-Commerce Backend

A microservices backend for an e-commerce order flow built on
**Spring Boot 3 · Java 21 · Apache Kafka · PostgreSQL**. Services communicate
asynchronously over Kafka domain events, and failures are recovered through
**compensating events** — giving eventual consistency across services without
distributed transactions or 2PC.

Every service is independently deployable, owns its own datastore, and ships
as a container. The full stack runs with a single `docker compose up --build`.

## Tech stack

| Layer          | Choice                                                  |
|----------------|---------------------------------------------------------|
| Language       | Java 21                                                 |
| Framework      | Spring Boot 3.3 (Web, Data JPA, Validation, Kafka)      |
| Messaging      | Apache Kafka (Confluent images), Zookeeper              |
| Persistence    | PostgreSQL 16 — one database per stateful service       |
| Build          | Maven (multi-module)                                    |
| Orchestration  | Docker Compose                                          |
| Utilities      | Lombok, Jackson                                         |

---

## Architecture

```
                          ┌─────────────────────────┐
                          │    notification-service  │
                          │  (consumes terminal      │
                          │   events, logs "sent")   │
                          └────────▲─────────▲───────┘
                                   │         │
                        order.     │         │ order.
                        confirmed  │         │ cancelled
                                   │         │
┌──────────────┐  order.created  ┌─┴─────────┴──┐  inventory.reserved  ┌──────────────────┐
│    client    ├───────────────▶ │ order-service │ ────────────────────▶│ payment-service  │
│  (REST POST) │◀───status──     │   (Postgres)  │                      │   (Postgres)     │
└──────────────┘                 └─────▲──┬──────┘                      └─────┬─────▲──────┘
                                       │  │                                    │     │
                         inventory.    │  │ order.created          payment.    │     │ payment.
                         failed /      │  │                        completed / │     │ failed
                         payment.      │  │                        failed      │     │
                         completed /   │  ▼                                    ▼     │
                         failed    ┌───┴──────────────┐                              │
                                   │ inventory-service │                              │
                                   │    (Postgres)     │◀─ payment.failed ────────────┘
                                   │   (seeded stock)  │   (compensating release)
                                   └───────────────────┘
```

## Services

| Service | Port | DB | Responsibility |
|---|---|---|---|
| **order-service** | 8081 | `orderdb` (5432) | REST entry point, owns order lifecycle, publishes terminal events |
| **inventory-service** | 8082 | `inventorydb` (5433) | Owns stock, reserves on order, releases on payment failure |
| **payment-service** | 8083 | `paymentdb` (5434) | Simulated payment processing (fails if `amount > 1000`) |
| **notification-service** | 8084 | – | Consumes terminal events and logs notifications |

## Kafka topics

| Topic | Produced by | Consumed by |
|---|---|---|
| `order.created` | order-service | inventory-service |
| `inventory.reserved` | inventory-service | payment-service |
| `inventory.failed` | inventory-service | order-service |
| `payment.completed` | payment-service | order-service |
| `payment.failed` | payment-service | order-service, inventory-service |
| `order.confirmed` | order-service | notification-service |
| `order.cancelled` | order-service | notification-service |

## Event flow

**Happy path**
```
POST /orders
  └─▶ order-service saves PENDING, publishes OrderCreated
      └─▶ inventory-service reserves stock, publishes InventoryReserved
          └─▶ payment-service charges, publishes PaymentCompleted
              └─▶ order-service updates CONFIRMED, publishes OrderConfirmed
                  └─▶ notification-service logs "order confirmed"
```

**Failure — out of stock**
```
OrderCreated → inventory-service → InventoryFailed
            → order-service sets CANCELLED, publishes OrderCancelled
            → notification-service logs "order cancelled"
```

**Failure — payment rejected (compensation)**
```
OrderCreated → InventoryReserved → payment-service → PaymentFailed
            → order-service sets CANCELLED, publishes OrderCancelled
            → inventory-service consumes PaymentFailed and RELEASES the stock
            → notification-service logs "order cancelled"
```

---

## Running locally

### Prerequisites
- Docker Desktop (or Docker Engine + Compose v2)
- Ports free: **2181, 9092, 29092, 5432, 5433, 5434, 8081–8084**

### Start everything
```bash
docker compose up --build
```
Brings up Zookeeper, Kafka, 3 Postgres instances, and all 4 services.

### Stop
```bash
docker compose down          # stop containers, keep volumes
docker compose down -v       # stop and wipe DB volumes
```

### Tail logs
```bash
docker compose logs -f order-service
docker compose logs -f inventory-service payment-service
```

### Health checks
```bash
curl localhost:8081/actuator/health
curl localhost:8082/actuator/health
curl localhost:8083/actuator/health
curl localhost:8084/actuator/health
```

---

## API reference

All public endpoints are on **order-service** (`localhost:8081`).

### `POST /orders` — place an order

Request:
```json
{
  "productId": "P1",
  "quantity": 2,
  "amount": 100
}
```

All three fields are required. `quantity` and `amount` must be positive;
`productId` must be non-blank. Validation errors return `400` with a structured
JSON body from the global exception handler.

Response `201 Created`:
```json
{
  "id": 1,
  "productId": "P1",
  "quantity": 2,
  "amount": 100,
  "status": "PENDING",
  "createdAt": "2026-04-21T10:00:00Z"
}
```

### `GET /orders/{id}` — fetch order status

Returns the same shape as above; `status` transitions to `CONFIRMED` or
`CANCELLED` within ~1s as the saga completes. `404` if the order does not exist.


## Project layout

```
event-driven-ecommerce/
├── pom.xml                       parent (Spring Boot 3.3 BOM, Java 21)
├── docker-compose.yml            Kafka + Zookeeper + 3x Postgres + 4 services
├── common-events/                shared Kafka event POJOs (used by all services)
├── order-service/                REST + producer + consumer
├── inventory-service/            consumer + pessimistic-locked stock reservation
├── payment-service/              consumer + deterministic pay/fail rule
└── notification-service/         terminal-event consumer, logs only
```

## Design decisions

- **Database per service.** Each stateful service owns its own Postgres; no cross-service joins or shared schema.
- **Async, event-driven communication.** Services never call each other directly; all coordination is via Kafka topics.
- **Compensating events for recovery.** Failed payment triggers stock release — no distributed transaction / 2PC.
- **Pessimistic locking on stock reservation.** Prevents oversell under concurrent orders against the same product.
- **Shared event-contracts module.** A single `common-events` Maven module defines every event POJO consumed across services, keeping schemas consistent.
- **JSON over Kafka.** Chose JSON + `spring.json.value.default.type` over Avro/Schema Registry to keep the demo focused on architecture.


## Roadmap

- Idempotency keys on consumers (at-least-once delivery → potential duplicates)
- Dead-letter topic + retry policy for poison messages
- Correlation/trace IDs propagated across services
- API Gateway (Spring Cloud Gateway) in front of public services
- JWT authentication via a dedicated auth service
- Schema Registry + Avro/Protobuf for type-safe contracts
- Distributed tracing (OpenTelemetry) and metrics (Prometheus/Grafana)
- Kubernetes manifests, HPA, and Kafka ACLs/SASL/SSL
- Integration tests with Testcontainers (Kafka + Postgres)
