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

Run `mvn spring-boot:run` three times, waiting for each run to finish:

1. The first run registers the demo data, creates `order-complete`, and leaves it at `IN_PROGRESS`.
   It prints a message asking you to run the application again.
2. The second run prints `order-complete loaded from database`, shows the stored mechanic and
   works, completes pending work, and prints the final price, `COMPLETED` status, and completion
   time.
3. The third run prints `order-complete loaded from database` and displays the persisted completed
   details without modifying the order.

The database connection defaults to `jdbc:postgresql://localhost:5432/carservice` and username
`carservice`. Set a local-only `DB_PASSWORD` in your shell before starting Compose and the app;
the password is intentionally not stored in the repository. `DB_URL` and `DB_USERNAME` can also
override their defaults. Stop PostgreSQL when finished with `docker compose down`; the named volume
preserves its data. To repeat the lifecycle from a clean database, run
`docker compose down -v && docker compose up -d`.

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
If Testcontainers cannot find a Docker environment on a newer Docker Engine, check the daemon's
minimum API version with `docker version` and create `~/.docker-java.properties` containing
`api.version=1.44` (use the daemon's minimum API version if it is higher).

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
(`spring.jpa.hibernate.ddl-auto=validate`). V1 (`V1__init_schema.sql`) creates customers, cars,
mechanics, repair orders, and service works, including enum and non-negative-cost checks, foreign
keys, and indexes. V2 (`V2__add_order_version.sql`) adds the non-null `repair_orders.version`
column; V3 (`V3__add_mechanic_version.sql`) adds the non-null `mechanics.version` column. The
`RepairOrder.works` relationship is a unidirectional `@OneToMany` with `@JoinColumn(name = "order_id")`
and `@OrderColumn(name = "position")` on its owning side. Keeping the order column on the owner
mapping lets Hibernate write both the foreign key and non-null list position when inserting work
rows. Do not edit an already-applied migration; add a new versioned migration for future changes.

## Transactions and optimistic locking

`RepairOrderService.complete` calculates the final price before invoking the domain's single
completion operation, which validates state, releases the mechanic, and records completion time;
the change is saved within one transaction. `addWorks` also forms a transaction boundary and
performs the work additions directly in its transactional method instead of calling another
service method through `this`; self-invocation bypasses Spring's proxy. Runtime exceptions cause
Spring to roll back the transaction by default.

`RepairOrder.version` is annotated with JPA `@Version`. Hibernate's update includes the previous
version in a predicate equivalent to
`UPDATE repair_orders SET ..., version = ? WHERE id = ? AND version = ?`. A concurrent update that
has already advanced the version makes the stale update affect no row; Spring reports
`ObjectOptimisticLockingFailureException`, typically during flush or commit. This is optimistic
locking: no database row lock is held during the edit, and conflicts are detected on write.
Pessimistic locking acquires a database lock up front and can block competing transactions. The
`OptimisticLockingIT` integration tests coordinate two transactions, verify the stale update fails,
and demonstrate that reloading before retrying succeeds.

`Mechanic.version` also uses `@Version`, so concurrent orders cannot both commit an assignment
based on the same free mechanic state. One competing transaction succeeds, while the stale
assignment fails with an optimistic-locking exception and leaves the mechanic assigned only once.

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
