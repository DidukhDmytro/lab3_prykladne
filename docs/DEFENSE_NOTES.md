# Defense Notes

## Java fundamentals

- **`final`, `finally`, and `finalize`:** `final` restricts reassignment, overriding, or
  inheritance depending on what it modifies. This project intentionally does not use it.
  `finally` is a `try` statement block intended for cleanup whether execution succeeds or throws.
  `finalize` was an unreliable garbage-collection callback, is deprecated, and is unrelated to
  `finally`; it should not be used for resource management.
- **Exception hierarchy:** `Throwable` is the root. Its main branches are `Error` and `Exception`.
  `RuntimeException` is under `Exception`. In this project, `DomainException` extends
  `RuntimeException`, and domain exceptions such as `InvalidArgumentException` extend
  `DomainException`. `PricingException` extends `IllegalArgumentException`, also an unchecked
  `RuntimeException`.
- **`Error`:** Indicates serious JVM or environment problems, such as `OutOfMemoryError`. It is
  generally not an application condition to catch and continue from.
- **Checked and unchecked exceptions:** Checked exceptions descend from `Exception` but not
  `RuntimeException`, so Java requires handling or declaration. Unchecked exceptions are
  `RuntimeException` (and its subclasses) and need not be declared. The project uses unchecked
  domain and pricing exceptions so failed business operations can propagate and cause transaction
  rollback.
- **`try` / `catch` / `finally`:** `try` contains an operation that may fail, `catch` handles a
  matching exception, and `finally` is normally executed after either path. For example,
  `CarServiceDemoRunner.runExpectedFailure` catches expected domain/pricing errors so the demo can
  continue. A `finally` block can be skipped by terminating the JVM with `System.exit` or
  `Runtime.halt`, a JVM crash, or a process kill; ordinary `return` and thrown exceptions do not
  normally skip it.

## Streams

- **Intermediate vs terminal operations:** Intermediate operations return another stream and are
  lazy until a terminal operation runs. Examples in this code include `filter`, `map`, and
  `sorted`. Terminal operations consume the stream and produce a result or side effect: `reduce`,
  `collect`, `anyMatch`, `allMatch`, and `forEach`.
- **Methods used in this project:**
  - `stream()` creates a stream over a collection. In `RepairOrder`, it starts processing work.
  - `map()` converts each work to a cost in `RepairOrder.calculatePartsTotal` and
    `calculateLaborTotal`, and converts works to display lines in `CarServiceDemoRunner`.
  - `reduce()` combines `BigDecimal` costs from `BigDecimal.ZERO` in those total methods; the
    pricing engine also reduces rule functions into an ordered pipeline.
  - `filter()` selects pending works before completing them in `CarServiceDemoRunner`.
  - `sorted()` orders pricing rules by priority in `PricingRuleEngine`.
  - `collect()` creates the defensive ordered pricing-rule copy in `PricingRuleEngine` and list
    snapshots in the in-memory repositories.
  - `anyMatch()` detects null pricing rules and unfinished work; `allMatch()` verifies mechanic
    compatibility in `PricingRuleEngine` and `RepairOrder`.
  - `forEach()` prints demo output and adds every batch work in `RepairOrderService.addWorks`.
  - `IntStream.range()` supplies valid work-list indexes for the runner's completion demonstration.
    It is a primitive stream, not a loop over a collection.

## Spring

- **Spring Framework and Spring Boot:** Spring Framework provides the container, dependency
  injection, transactions, and other application infrastructure. Spring Boot builds on Spring
  Framework with auto-configuration, starter dependencies, and a convenient application launcher.
  `CarServiceApplication` starts the Boot app; `application.yml` configures PostgreSQL, Flyway, and
  non-web execution.
- **IoC, DI, and bean:** Inversion of Control means the application framework manages object
  creation and lifecycle. Dependency Injection supplies an object's collaborators instead of
  having it construct them. A bean is an object managed by Spring. Services receive dependencies
  through constructors, for example `RepairOrderService`.
- **Bean annotations:** `@Component` marks a general Spring-managed class;
  `@Service` marks service-layer components; `@Repository` marks persistence components and may
  translate persistence exceptions; `@Configuration` declares bean configuration; and `@Bean`
  marks a factory method whose return value is registered in the context. `PricingConfiguration`
  defines `Clock` and `PricingPolicy` beans. `CarServiceDemoRunner` is a component, and
  `RepairOrderService` and `RegistrationService` are services.
- **Transactions and self-invocation:** Spring's `@Transactional` is applied through a proxy around
  `RepairOrderService` calls. `addWorks` is itself transactional and performs the whole batch
  directly. Calling another annotated method through `this` would not cross the proxy, so
  self-invocation would not establish the intended proxy-based boundary. Runtime exceptions cause
  rollback by default; checked exceptions do not unless configured. Completion and batch failures
  are unchecked, so their database changes roll back.

## Persistence

- **JPA, Hibernate, Spring Data JPA:** JPA is the Java persistence specification and API.
  Hibernate is the ORM implementation used by this project. Spring Data JPA builds repository
  abstractions and query support on top of JPA. The repository interfaces extend Spring Data's
  `Repository`; Spring Data creates their runtime implementations.
- **`@Entity` and `@Id`:** `@Entity` marks a class as persistent and `@Id` identifies its primary
  key. `Customer`, `Car`, `Mechanic`, `RepairOrder`, and `ServiceWork` are entities. The business
  string keys are assigned by the application; `ServiceWork.id` is database-generated.
- **Relationships and owning side:** `Car.owner` is a `@ManyToOne` relationship with the
  `owner_id` foreign key. `RepairOrder.car` and `RepairOrder.assignedMechanic` are also
  `@ManyToOne` relationships, using `car_vin` and `mechanic_id`. `ServiceWork.order` owns the work
  relationship because it stores `order_id`; `RepairOrder.works` is the inverse side with
  `mappedBy = "order"`, cascading and ordered by `position`. Updating both sides when adding or
  removing work keeps the object graph consistent.
- **LAZY and EAGER:** LAZY loading defers fetching a relationship until access; EAGER loading loads
  it with the entity query. The entity relationships here use LAZY loading. To safely access
  required relations after a repository transaction, `RepairOrderRepository.findById` uses
  `@EntityGraph` for works, car, car owner, and mechanic. Its status and all-order queries use
  `JOIN FETCH` for works and the status query also fetches car and mechanic.
- **Flyway:** Flyway applies ordered, versioned SQL migrations and records applied versions in its
  schema-history table. V1 creates the car-service schema; V2 adds `repair_orders.version`.
  Hibernate is configured to validate the migrated schema, not create it.
- **ACID:** Atomicity means all-or-nothing; Consistency preserves constraints and invariants;
  Isolation separates concurrent transactions; Durability preserves committed data. PostgreSQL
  transactions and the service transaction boundaries provide these properties.
- **Optimistic vs pessimistic locking:** `RepairOrder.version` has `@Version`. Hibernate issues an
  update with both id and previous version in the predicate, equivalent to
  `UPDATE ... WHERE id = ? AND version = ?`, and increments the version on success. If no row
  matches, `ObjectOptimisticLockingFailureException` is typically raised at flush or commit.
  Optimistic locking holds no database row lock throughout the edit and detects conflicts on write;
  pessimistic locking obtains a database lock up front and can make other transactions wait.
  `OptimisticLockingIT` coordinates two transactions to show a stale update failing and a reload
  followed by retry succeeding. In-memory repositories remain available for focused unit tests.
