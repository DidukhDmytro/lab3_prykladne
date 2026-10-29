# Car Service

A Java 17 console application for registering customers, cars, and mechanics, managing repair
orders, calculating prices, and persisting data in PostgreSQL.

## Requirements

- JDK 17
- Apache Maven
- Docker with Docker Compose (for PostgreSQL and the Testcontainers integration tests)

## Run the application

Start PostgreSQL:

```sh
docker compose up -d
```

Run the app:

```sh
mvn spring-boot:run
```

Run the same command a second time after the first run finishes. The runner is idempotent: the
second run prints `order-complete loaded from database`, followed by the stored order, works,
completion status, price, and timestamp. This confirms that the order was loaded from PostgreSQL,
not recreated. The database connection defaults to `jdbc:postgresql://localhost:5432/carservice`
with user/password `carservice`; set `DB_URL`, `DB_USERNAME`, or `DB_PASSWORD` to override them.
Stop PostgreSQL when finished with `docker compose down`; the named volume preserves its data.

## Tests

Run unit tests (no Docker required):

```sh
mvn clean test
```

Run the full suite, including PostgreSQL integration tests (Docker required):

```sh
mvn clean verify
```

Testcontainers starts PostgreSQL for integration tests. Integration tests are intentionally not
wrapped in test-managed transactions, so service transaction commits and rollbacks are exercised.

## Project structure

```text
src/
├── main/java/com/example/carservice/
│   ├── app/          # Spring CommandLineRunner and demo pricing rules
│   ├── config/       # Application constants and pricing/clock beans
│   ├── domain/       # JPA-mapped domain objects and business rules
│   ├── exception/    # Domain and application exceptions/messages
│   ├── pricing/      # Pricing policy, engine, contexts, and rules
│   ├── repository/   # Spring Data repository interfaces and memory adapters
│   └── service/      # Registration, repair-order, and gift services
├── main/resources/
│   └── db/migration/ # Flyway SQL migrations
└── test/java/com/example/carservice/
    ├── domain/       # Domain invariant tests
    ├── integration/  # PostgreSQL/Testcontainers integration tests
    ├── pricing/      # Pricing tests
    ├── repository/   # In-memory repository tests
    └── service/      # Service unit tests
```

## Architecture

- **Domain:** `Customer`, `Car`, `Mechanic`, `RepairOrder`, and `ServiceWork` hold business state
  and enforce invariants and transitions. These same domain classes are the JPA entities; gifts and
  presents are not entities.
- **Services and transactions:** `RegistrationService` and `RepairOrderService` use constructor
  injection. `RepairOrderService` is `@Transactional`; read operations are read-only. Completion
  updates order status, releases its mechanic, calculates the final price, records the completion
  time, and saves as one transaction. `addWorks` validates and adds the batch in one transaction
  and saves once. Runtime exceptions trigger Spring's default rollback behavior, so failures do not
  persist partial completion or batch changes.
- **Repositories:** `CustomerRepository`, `CarRepository`, `MechanicRepository`, and
  `RepairOrderRepository` are Spring Data repository interfaces. Missing lookups return
  `Optional.empty()`. `RepairOrderRepository` uses `@EntityGraph` and fetch-join queries for
  relationships needed outside a transaction. The `repository.memory` implementations remain for
  unit tests and are not registered as application beans.
- **Pricing:** `PricingPolicy` is the service-facing abstraction. `PriceCalculator` adapts order
  totals into `PricingContext`; `PricingRuleEngine` orders and applies rules, clamps negative
  results, applies the minimum, and rounds once to two decimals.
- **Runner:** `CarServiceDemoRunner` is a `CommandLineRunner` active outside the `test` profile. It
  demonstrates registration, order lifecycle and persistence, pricing, expected error cases, and
  an optimistic-lock conflict.

## Database schema and migrations

Flyway owns schema changes; Hibernate validates the resulting schema
(`spring.jpa.hibernate.ddl-auto=validate`). V1 creates customers, cars, mechanics, repair orders,
and ordered service works, including enum and non-negative-cost checks, foreign keys, and indexes.
V2 adds the non-null `repair_orders.version` column used for optimistic locking. Do not edit an
already-applied migration; add a new versioned migration for future schema changes.

## Transactions and optimistic locking

`RepairOrderService.complete` and `addWorks` each form a transaction boundary. If a runtime
exception occurs before commit, Spring rolls back the transaction by default. `addWorks` performs
the work additions directly in its transactional method instead of calling another service method
through `this`; self-invocation bypasses Spring's proxy and would not create a separate proxy-based
transaction boundary.

`RepairOrder.version` is annotated with JPA `@Version`. Hibernate's update includes the previous
version in a predicate equivalent to
`UPDATE repair_orders SET ..., version = ? WHERE id = ? AND version = ?`. A concurrent update that
has already advanced the version makes the stale update affect no row; Spring reports
`ObjectOptimisticLockingFailureException`, typically during flush or commit. This is optimistic
locking: no database row lock is held during the edit, and conflicts are detected on write.
Pessimistic locking acquires a database lock up front and can block competing transactions. The
`OptimisticLockingIT` integration tests coordinate two transactions, verify the stale update fails,
and demonstrate that reloading before retrying succeeds.

## Business rules

| Rule | Enforced by |
|---|---|
| Car VIN and owner are required | `Car` constructor |
| Customer id, name, and phone number are required | `Customer` constructor |
| Mechanic id, name, and specialization are required; mechanics start free | `Mechanic` constructor |
| Work description, costs, and specialization are valid; zero costs are allowed | `ServiceWork` constructor |
| An assigned mechanic must be compatible with all work | `RepairOrder.addWork` and `RepairOrder.assignMechanic` |
| Approval requires work; progress requires approval and an assigned mechanic | `RepairOrder.approve` and `RepairOrder.startProgress` |
| Completion requires all work complete; completion and cancellation release the mechanic | `RepairOrder.complete` and `RepairOrder.cancel` |
| Completed or cancelled orders cannot be modified | `RepairOrder` modification operations |
| Urgent priority adds a surcharge to labor only | `UrgentSurchargeRule` |
| Parts-total gift tiers select washer fluid, interior cleaning, or a voucher | `GiftPolicy` |
