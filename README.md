# Car Service

A Java application for modeling car-service customers, cars, mechanics, repair orders, work,
pricing, and customer gifts. The project uses Maven's standard source layout and includes a
console demonstration and JUnit 5 tests.

## Requirements

- JDK 17 or newer
- Apache Maven 3.9 or compatible

## Commands

Run the test suite:

```sh
mvn clean test
```

Compile the application and run the console demo:

```sh
mvn compile exec:java
```

## Project structure

```text
src/
├── main/java/com/example/carservice/
│   ├── app/          # Demo and demonstration pricing rules
│   ├── config/       # Shared application constants
│   ├── domain/       # Repair-order and related business objects
│   ├── exception/    # Domain and application exceptions/messages
│   ├── pricing/      # Pricing pipeline and reusable rules
│   ├── repository/   # Repository interfaces and in-memory adapters
│   └── service/      # Application services and gift policy
└── test/java/com/example/carservice/
    ├── domain/
    ├── pricing/
    ├── repository/
    └── service/
```

## Architecture

- **Domain:** `RepairOrder`, `ServiceWork`, `Mechanic`, `Car`, and `Customer` model the business
  state and enforce their own invariants and allowed state transitions.
- **Repository:** interfaces define storage operations; `repository.memory` provides
  `HashMap` implementations for `CustomerRepository`, `CarRepository`,
  `MechanicRepository`, and `RepairOrderRepository`. Services depend on these interfaces, so
  application behavior is independent of storage details and repositories can be replaced or
  mocked. In-memory `findAll()` and `findByStatus()` calls return unmodifiable copies in
  unspecified order; callers must not rely on the order. Saving a customer, mechanic, or repair
  order with an existing id replaces its stored entry. In contrast, `InMemoryCarRepository.save`
  throws `EntityAlreadyExistsException` when the VIN already exists.
- **Service:** `RepairOrderService` coordinates repository access and domain operations.
  `RegistrationService` handles customer, car, and mechanic registration, and `GiftPolicy` selects
  gifts by parts total. `RegistrationService.registerMechanic` does not check for duplicate
  mechanic ids; saving an existing id replaces its stored entry.
- **Pricing:** `PricingRuleEngine` applies `PricingRule`s to a non-null `PricingContext` in ascending
  priority order. It requires a non-null rule list without null elements, a non-negative minimum
  price, and non-null results from every rule; invalid input raises `PricingException`.
  `PricingContext` can be created directly from pricing totals and flags without a `RepairOrder`,
  and the engine can run independently of the service domain.
  Negative rule results are clamped to zero before applying the minimum price. The result is never
  null or below the minimum and is rounded exactly once at the end to scale 2 using
  `RoundingMode.HALF_UP`. The engine does not mutate inputs, copies and sorts the rules once at
  construction, and repeated calls with equal context return equal results when rules are
  stateless and deterministic. Fixed and percentage discounts are order-sensitive because fixed
  discounts subtract an amount before/after percentage scaling; two percentage discounts commute
  because each scales the total by a factor, and multiplying those factors gives the same result
  in either order. `PricingPolicy` is the service-facing abstraction, and `PriceCalculator` adapts
  `RepairOrder` data into `PricingContext`. `PricingException` extends
  `IllegalArgumentException`, not `DomainException`, so pricing errors are not caught by handlers
  that catch only `DomainException`.
- **Missing orders:** repository lookups return `Optional.empty()` when no order is found.
  `RepairOrderService` converts that result to `EntityNotFoundException` when an order is required.

## Design rationale

- `RepairOrderRepository` and `PricingPolicy`/`PricingRule` invert dependencies, allow storage and
  pricing implementations to be replaced, and support mocks in tests; new pricing rules can be
  added without changing the engine.
- `GiftPolicy` has no interface because it is a small, single implementation with no current need
  for interchangeable policies.
- Services call `repository.save()` after each domain operation to preserve the persistence boundary
  for replaceable repositories, even though the in-memory repository holds the same object instance.
- `OldCarDiscountRule` receives the current year in its constructor so its age calculation is
  deterministic in tests.

## Business rules

| Rule | Enforced by |
|---|---|
| Car VIN and owner are required | `Car` constructor |
| Customer id, name, and phone number are required | `Customer` constructor |
| Mechanic id, name, and specialization are required; mechanics start free | `Mechanic` constructor |
| Work description, costs, and specialization are valid; zero costs are allowed and negative costs are rejected | `ServiceWork` constructor |
| Repair-order id and car are required | `RepairOrder` constructor |
| Work and assigned mechanics are required when added or assigned; an assigned mechanic must be compatible with all work, and later work must remain compatible | `RepairOrder.addWork` and `RepairOrder.assignMechanic` |
| Busy mechanics cannot be assigned to another order | `Mechanic.assignToOrder` |
| Approval requires at least one work item | `RepairOrder.approve` |
| Progress requires an approved order and an assigned mechanic | `RepairOrder.startProgress` |
| Individual work can be completed only on an in-progress order and only at a valid work-list index | `RepairOrder.completeWork` |
| Completion requires an in-progress order and all work completed; completing or cancelling releases its mechanic | `RepairOrder.complete` and `RepairOrder.cancel` |
| Completed or cancelled orders cannot be modified | `RepairOrder` modification operations |
| Duplicate customer ids, car VINs, and repair-order ids are rejected | `RegistrationService.registerCustomer`, `InMemoryCarRepository.save`, and `RepairOrderService.createOrder` |
| Saving a customer, mechanic, or repair order with an existing id replaces its stored entry | In-memory repository `save` operations; `RegistrationService.registerMechanic` does not check duplicates |
| A car can be registered only when its owner is a registered customer | `RegistrationService.registerCar` |
| Urgent priority adds a surcharge to labor only | `UrgentSurchargeRule` |
| Parts-total gift tiers select washer fluid, interior cleaning, or a discount voucher | `GiftPolicy` |
| Pricing-rule constructor parameters are validated; old-car discounts use the injected current year and age limit | Pricing rule constructors |
| Pricing applies rules in ascending stable priority order, clamps negative totals, rounds once with `HALF_UP`, and respects a minimum price | `PricingRuleEngine` |
