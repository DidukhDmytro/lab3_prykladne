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

1. The first run registers the demo customer, car, and mechanic, creates the order with its fixed
   UUID, leaves it at `IN_PROGRESS`, and prints its details and a restart message.
2. The second run prints the fixed order UUID with `loaded from database`, shows the stored
   mechanic and works, completes pending work, and prints the final price, `COMPLETED` status, and
   completion time.
3. The third run prints the fixed order UUID with `loaded from database` and displays the persisted
   completed details without modifying the order.

Every run also prints pricing demonstrations for normal and urgent pricing, discount-rule order,
a custom surcharge, and an old-car discount.

The application connects to `jdbc:postgresql://localhost:5432/carservice` with username `carservice`
and password `carservice`; these are local development credentials defined in `docker-compose.yml`
and `src/main/resources/application.yml`. `DB_URL` and `DB_USERNAME` can override the URL and
username through environment variables. The password is not configurable through the environment.
Stop PostgreSQL when finished with `docker compose down`; the named volume preserves its data. To
recreate an existing local database after these migration changes, run
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

The integration-test Spring configuration starts PostgreSQL through a `@ServiceConnection` bean.
`IntegrationTest` runs `src/test/resources/sql/cleanup.sql` before each test. Integration tests are
not wrapped in test-managed transactions, so service transaction commits, rollbacks, and concurrent
updates are exercised.
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
│   │   └── enumerate/ # Domain and pricing enums
│   ├── exception/    # Domain and application exceptions/messages
│   ├── pricing/      # Pricing policy, engine, contexts, and rules
│   ├── repository/   # Spring Data repository interfaces
│   └── service/      # Registration, repair-order, and gift services
├── main/resources/
│   └── db/migration/ # Flyway SQL migrations
├── test/java/com/example/carservice/
│   ├── domain/       # Domain invariant tests
│   ├── integration/  # PostgreSQL/Testcontainers integration tests and configuration
│   ├── pricing/      # Pricing tests
│   ├── repository/   # Repository tests
│   └── service/      # Service unit tests
└── test/resources/
    └── sql/
        └── cleanup.sql  # Integration-test database cleanup
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
  relationships needed outside a transaction.
- **Pricing:** `PricingPolicy` is the service-facing abstraction. `PriceCalculator` adapts order
  totals into `PricingContext`; `PricingRuleEngine` orders and applies rules, clamps negative
  results, applies the minimum, and rounds once to two decimals.
- **Runner:** `CarServiceDemoRunner` is a `CommandLineRunner` active outside the `test` profile. It
  demonstrates registration, the persisted three-run order lifecycle, and pricing examples.

## Database schema and migrations

Flyway owns schema changes; Hibernate validates the resulting schema
(`spring.jpa.hibernate.ddl-auto=validate`). Existing local databases must be recreated with
`docker compose down -v && docker compose up -d` because V1-V3 have changed in place. V1
(`V1__init_schema.sql`) creates customers, cars,
mechanics, repair orders, and service works, including enum and non-negative-cost checks, foreign
keys, and indexes. V2 (`V2__add_order_version.sql`) adds the non-null `repair_orders.version`
column; V3 (`V3__add_mechanic_version.sql`) adds the non-null `mechanics.version` column. The
`RepairOrder.works` relationship is a unidirectional `@OneToMany` with `@JoinColumn(name = "order_id")`
and `@OrderColumn(name = "position")` on its owning side. Keeping the order column on the owner
mapping lets Hibernate write both the foreign key and non-null list position when inserting work
rows.

## Transactions and optimistic locking

`RepairOrderService.complete` loads the order, validates that it can be completed, calculates the
final price, completes the order, and saves it within one transaction. The domain operation also
validates completion before releasing the mechanic and recording the completion time.
`addWorks` also forms a transaction boundary and
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
