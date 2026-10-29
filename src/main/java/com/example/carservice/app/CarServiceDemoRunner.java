package com.example.carservice.app;

import com.example.carservice.config.AppConstants;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.WorkStatus;
import com.example.carservice.exception.DomainException;
import com.example.carservice.pricing.PriceCalculator;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.pricing.rule.FixedDiscountRule;
import com.example.carservice.pricing.rule.PercentageDiscountRule;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("!test")
public class CarServiceDemoRunner implements CommandLineRunner {
    private static String customerId = "customer-001";
    private static String customerName = "Alex Morgan";
    private static String customerPhone = "+1-555-0100";
    private static String carVin = "DEMO-VIN-001";
    private static String carMake = "Example Motors";
    private static String carModel = "Roadster";
    private static int carYear = 2010;
    private static int currentYear = 2026;
    private static int oldCarAgeLimit = 10;
    private static String mechanicId = "mechanic-general";
    private static String mechanicName = "Taylor General";
    private static String engineMechanicId = "mechanic-engine";
    private static String engineMechanicName = "Jordan Engine";
    private static String brakesMechanicId = "mechanic-brakes";
    private static String brakesMechanicName = "Casey Brakes";
    private static String demoOrderId = "order-complete";
    private static String normalPricingOrderId = "pricing-normal";
    private static String urgentPricingOrderId = "pricing-urgent";
    private static String invalidTransitionOrderId = "order-demo-invalid-transition";
    private static String batchOrderId = "order-demo-batch-rollback";
    private static String missingOrderId = "order-demo-missing";
    private static String optimisticOrderId = "order-demo-optimistic-lock";
    private static String restartInstruction =
            "Run the application again to load and complete this order.";
    private static String registrationFormat = "%s -> %s";
    private static String loadedOrderFormat = "%s loaded from database -> status: %s";
    private static String mechanicFormat = "Assigned mechanic: %s";
    private static String orderStatusFormat = "Order status: %s";
    private static String noMechanicLabel = "none";
    private static String workFormat = "Work: %s | status: %s | parts: %s | labor: %s";
    private static String completionFormat =
            "Completed order -> final price: %s; status: %s; completed at: %s";
    private static String priceFormat = "%s -> base: %s; final: %s";
    private static String expectedErrorFormat = "%s -> expected error: %s";
    private static String missingExpectedErrorFormat = "%s -> expected error did not occur";
    private static String beforeCountFormat = "Batch work count before: %s";
    private static String afterCountFormat = "Batch work count after: %s";
    private static String createdOrderLabel = "Created demo order";
    private static String customerRegisteredLabel = "Customer registered";
    private static String carRegisteredLabel = "Car registered";
    private static String mechanicRegisteredLabel = "Mechanic registered";
    private static String normalPricingLabel = "Normal pricing";
    private static String urgentPricingLabel = "Urgent surcharge";
    private static String fixedThenPercentLabel = "Fixed discount then percentage discount";
    private static String percentThenFixedLabel = "Percentage discount then fixed discount";
    private static String oldCarPricingLabel = "Old-car discount";
    private static String customSurchargeLabel = "Custom surcharge before percentage discount";
    private static String invalidTransitionLabel = "Completing an order before progress";
    private static String missingOrderLabel = "Loading a missing order";
    private static String batchFailureLabel = "Adding a batch with an invalid last work item";
    private static String duplicateOrderLabel = "Creating a duplicate order";
    private static String optimisticLockLabel = "Concurrent order priority update";
    private static String optimisticLockFailureFormat = "%s -> expected optimistic-lock conflict: %s";
    private static String missingOptimisticLockFormat = "%s -> expected optimistic-lock conflict did not occur";
    private static String optimisticLockOrderMissingMessage = "Optimistic-lock demo order was not found.";
    private static String latchTimeoutMessage = "Timed out waiting for the competing transaction.";
    private static String staleWriterThreadName = "demo-optimistic-stale-writer";
    private static String engineDescription = "Engine service";
    private static String brakesDescription = "Brake service";
    private static String batchDescription = "Batch rollback check";
    private static BigDecimal enginePartsCost = new BigDecimal("200.00");
    private static BigDecimal engineLaborCost = new BigDecimal("85.00");
    private static BigDecimal brakesPartsCost = new BigDecimal("60.00");
    private static BigDecimal brakesLaborCost = new BigDecimal("45.00");
    private static BigDecimal batchPartsCost = new BigDecimal("10.00");
    private static BigDecimal batchLaborCost = new BigDecimal("5.00");
    private static BigDecimal discountPercent = new BigDecimal("10");
    private static BigDecimal oldCarDiscountPercent = new BigDecimal("5");
    private static BigDecimal fixedDiscountAmount = new BigDecimal("100.00");
    private static BigDecimal customSurchargeAmount = new BigDecimal("25.00");
    private static int firstWorkIndex = 0;
    private static int latchInitialCount = 1;
    private static long coordinationTimeoutSeconds = 10;
    private static long threadJoinTimeoutMillis = 10000;

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;
    private PricingPolicy pricingPolicy;
    private TransactionTemplate transactionTemplate;

    public CarServiceDemoRunner(
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            PricingPolicy pricingPolicy,
            PlatformTransactionManager transactionManager) {
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
        this.pricingPolicy = pricingPolicy;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        Customer customer = ensureCustomer();
        Car car = ensureCar(customer);
        Mechanic mechanic = ensureMechanic(
                mechanicId,
                mechanicName,
                MechanicSpecialization.GENERAL);
        ensureMechanic(
                engineMechanicId,
                engineMechanicName,
                MechanicSpecialization.ENGINE);
        ensureMechanic(
                brakesMechanicId,
                brakesMechanicName,
                MechanicSpecialization.BRAKES);
        Optional<RepairOrder> existingOrder = repairOrderService.findOrder(demoOrderId);
        if (existingOrder.isPresent()) {
            handleExistingDemoOrder(existingOrder.get());
        } else {
            RepairOrder order = createDemoOrder(car, mechanic);
            printOrderDetails(order);
            System.out.println(restartInstruction);
        }
        runPricingDemonstrations(car);
        runNegativeScenarios(car);
        runOptimisticLockingScenario(car);
    }

    private Customer ensureCustomer() {
        Optional<Customer> existing = registrationService.findCustomer(customerId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Customer customer = new Customer(customerId, customerName, customerPhone);
        registrationService.registerCustomer(customer);
        System.out.println(String.format(registrationFormat, customerRegisteredLabel, customerId));
        return customer;
    }

    private Car ensureCar(Customer customer) {
        Optional<Car> existing = registrationService.findCar(carVin);
        if (existing.isPresent()) {
            return existing.get();
        }
        Car car = new Car(carVin, carMake, carModel, carYear, customer);
        registrationService.registerCar(car);
        System.out.println(String.format(registrationFormat, carRegisteredLabel, carVin));
        return car;
    }

    private Mechanic ensureMechanic(
            String id,
            String name,
            MechanicSpecialization specialization) {
        Optional<Mechanic> existing = registrationService.findMechanic(id);
        if (existing.isPresent()) {
            return existing.get();
        }
        Mechanic mechanic = new Mechanic(
                id,
                name,
                specialization);
        registrationService.registerMechanic(mechanic);
        System.out.println(String.format(registrationFormat, mechanicRegisteredLabel, id));
        return mechanic;
    }

    private RepairOrder createDemoOrder(Car car, Mechanic mechanic) {
        repairOrderService.createOrder(demoOrderId, car.getVin());
        repairOrderService.addWorks(demoOrderId, List.of(createEngineWork(), createBrakesWork()));
        repairOrderService.diagnose(demoOrderId);
        repairOrderService.approve(demoOrderId);
        repairOrderService.assignMechanic(demoOrderId, mechanic.getId());
        repairOrderService.startProgress(demoOrderId);
        RepairOrder createdOrder = repairOrderService.getOrder(demoOrderId);
        System.out.println(String.format(registrationFormat, createdOrderLabel, demoOrderId));
        return createdOrder;
    }

    private void handleExistingDemoOrder(RepairOrder order) {
        System.out.println(String.format(loadedOrderFormat, demoOrderId, order.getStatus()));
        printOrderDetails(order);
        if (order.getStatus() == OrderStatus.IN_PROGRESS) {
            completeDemoOrder(order);
        } else if (order.getStatus() == OrderStatus.COMPLETED) {
            printCompletion(order);
        }
    }

    private void printOrderDetails(RepairOrder order) {
        System.out.println(String.format(orderStatusFormat, order.getStatus()));
        Mechanic mechanic = order.getAssignedMechanic();
        System.out.println(String.format(
                mechanicFormat,
                mechanic == null ? noMechanicLabel : mechanic.getFullName()));
        order.getWorks().stream()
                .map(work -> String.format(
                        workFormat,
                        work.getDescription(),
                        work.getStatus(),
                        work.getPartsCost(),
                        work.getLaborCost()))
                .forEach(System.out::println);
    }

    private void completeDemoOrder(RepairOrder order) {
        List<ServiceWork> works = order.getWorks();
        IntStream.range(firstWorkIndex, works.size())
                .filter(index -> works.get(index).getStatus() == WorkStatus.PENDING)
                .forEach(index -> repairOrderService.completeWork(demoOrderId, index));
        repairOrderService.complete(demoOrderId);
        printCompletion(repairOrderService.getOrder(demoOrderId));
    }

    private void printCompletion(RepairOrder order) {
        System.out.println(String.format(
                completionFormat,
                order.getFinalPrice(),
                order.getStatus(),
                order.getCompletedAt()));
    }

    private void runPricingDemonstrations(Car car) {
        RepairOrder normalOrder = createPricingOrder(normalPricingOrderId, car, false);
        printPrice(normalPricingLabel, normalOrder);
        RepairOrder urgentOrder = createPricingOrder(urgentPricingOrderId, car, true);
        printPrice(urgentPricingLabel, urgentOrder);

        PriceCalculator fixedThenPercentCalculator = new PriceCalculator(
                List.of(
                        new FixedDiscountRule(fixedDiscountAmount),
                        new PercentageDiscountRule(discountPercent)),
                AppConstants.minimumPrice());
        PriceCalculator percentThenFixedCalculator = new PriceCalculator(
                List.of(
                        new PercentageDiscountRule(discountPercent),
                        new FixedDiscountRule(fixedDiscountAmount)),
                AppConstants.minimumPrice());
        System.out.println(String.format(
                priceFormat,
                fixedThenPercentLabel,
                fixedThenPercentCalculator.calculateBasePrice(normalOrder),
                fixedThenPercentCalculator.calculateFinalPrice(normalOrder)));
        System.out.println(String.format(
                priceFormat,
                percentThenFixedLabel,
                percentThenFixedCalculator.calculateBasePrice(normalOrder),
                percentThenFixedCalculator.calculateFinalPrice(normalOrder)));
        PriceCalculator customStageCalculator = new PriceCalculator(
                List.of(
                        new PercentageDiscountRule(discountPercent),
                        new DemonstrationSurchargeRule(customSurchargeAmount)),
                AppConstants.minimumPrice());
        System.out.println(String.format(
                priceFormat,
                customSurchargeLabel,
                customStageCalculator.calculateBasePrice(normalOrder),
                customStageCalculator.calculateFinalPrice(normalOrder)));

        PriceCalculator oldCarCalculator = new PriceCalculator(
                List.of(new OldCarDiscountRule(
                        currentYear,
                        oldCarAgeLimit,
                        oldCarDiscountPercent)),
                AppConstants.minimumPrice());
        System.out.println(String.format(
                priceFormat,
                oldCarPricingLabel,
                oldCarCalculator.calculateBasePrice(normalOrder),
                oldCarCalculator.calculateFinalPrice(normalOrder)));
    }

    private void printPrice(String label, RepairOrder order) {
        System.out.println(String.format(
                priceFormat,
                label,
                pricingPolicy.calculateBasePrice(order),
                pricingPolicy.calculateFinalPrice(order)));
    }

    private RepairOrder createPricingOrder(String id, Car car, boolean urgent) {
        RepairOrder order = new RepairOrder(id, car);
        order.addWork(createEngineWork());
        order.addWork(createBrakesWork());
        if (urgent) {
            order.changePriority(OrderPriority.URGENT);
        }
        return order;
    }

    private void runNegativeScenarios(Car car) {
        runExpectedFailure(
                duplicateOrderLabel,
                () -> repairOrderService.createOrder(demoOrderId, car.getVin()));
        runExpectedFailure(invalidTransitionLabel, () -> {
            ensureNegativeOrder(invalidTransitionOrderId, car);
            repairOrderService.complete(invalidTransitionOrderId);
        });
        runExpectedFailure(missingOrderLabel, () -> repairOrderService.getOrder(missingOrderId));
        runBatchRollbackScenario(car);
    }

    private void runOptimisticLockingScenario(Car car) {
        ensureNegativeOrder(optimisticOrderId, car);
        CountDownLatch staleReadComplete = new CountDownLatch(latchInitialCount);
        CountDownLatch firstCommitComplete = new CountDownLatch(latchInitialCount);
        AtomicReference<RuntimeException> staleTransactionFailure = new AtomicReference<>();
        Thread staleWriter = new Thread(() -> {
            try {
                transactionTemplate.execute(status -> {
                    repairOrderService.findOrder(optimisticOrderId)
                            .orElseThrow(() -> new IllegalStateException(optimisticLockOrderMissingMessage));
                    staleReadComplete.countDown();
                    await(firstCommitComplete);
                    repairOrderService.changePriority(optimisticOrderId, OrderPriority.URGENT);
                    return null;
                });
            } catch (RuntimeException exception) {
                staleTransactionFailure.set(exception);
            }
        }, staleWriterThreadName);
        staleWriter.start();
        boolean staleReadCompleted;
        try {
            staleReadCompleted = staleReadComplete.await(coordinationTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(latchTimeoutMessage, exception);
        }
        if (!staleReadCompleted) {
            firstCommitComplete.countDown();
            joinStaleWriter(staleWriter);
            throw new IllegalStateException(latchTimeoutMessage);
        }
        try {
            repairOrderService.changePriority(optimisticOrderId, OrderPriority.URGENT);
        } finally {
            firstCommitComplete.countDown();
        }
        joinStaleWriter(staleWriter);
        try {
            RuntimeException failure = staleTransactionFailure.get();
            if (failure != null) {
                throw failure;
            }
            System.out.println(String.format(missingOptimisticLockFormat, optimisticLockLabel));
        } catch (ObjectOptimisticLockingFailureException exception) {
            System.out.println(String.format(
                    optimisticLockFailureFormat,
                    optimisticLockLabel,
                    exception.getMessage()));
        }
        transactionTemplate.execute(status ->
                repairOrderService.changePriority(optimisticOrderId, OrderPriority.NORMAL));
    }

    private void runBatchRollbackScenario(Car car) {
        ensureNegativeOrder(batchOrderId, car);
        int beforeCount = repairOrderService.getOrder(batchOrderId).getWorks().size();
        System.out.println(String.format(beforeCountFormat, beforeCount));
        runExpectedFailure(batchFailureLabel, () -> repairOrderService.addWorks(
                batchOrderId,
                Stream.of(createBatchWork(), null).toList()));
        int afterCount = repairOrderService.getOrder(batchOrderId).getWorks().size();
        System.out.println(String.format(afterCountFormat, afterCount));
    }

    private void ensureNegativeOrder(String orderId, Car car) {
        if (repairOrderService.findOrder(orderId).isEmpty()) {
            repairOrderService.createOrder(orderId, car.getVin());
        }
    }

    private void await(CountDownLatch latch) {
        try {
            boolean released = latch.await(coordinationTimeoutSeconds, TimeUnit.SECONDS);
            if (!released) {
                throw new IllegalStateException(latchTimeoutMessage);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(latchTimeoutMessage, exception);
        }
    }

    private void joinStaleWriter(Thread staleWriter) {
        try {
            staleWriter.join(threadJoinTimeoutMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(latchTimeoutMessage, exception);
        }
        if (staleWriter.isAlive()) {
            throw new IllegalStateException(latchTimeoutMessage);
        }
    }

    private void runExpectedFailure(String label, Runnable operation) {
        try {
            operation.run();
            System.out.println(String.format(missingExpectedErrorFormat, label));
        } catch (DomainException | PricingException exception) {
            System.out.println(String.format(expectedErrorFormat, label, exception.getMessage()));
        }
    }

    private ServiceWork createEngineWork() {
        return new ServiceWork(
                engineDescription,
                enginePartsCost,
                engineLaborCost,
                MechanicSpecialization.ENGINE);
    }

    private ServiceWork createBrakesWork() {
        return new ServiceWork(
                brakesDescription,
                brakesPartsCost,
                brakesLaborCost,
                MechanicSpecialization.BRAKES);
    }

    private ServiceWork createBatchWork() {
        return new ServiceWork(
                batchDescription,
                batchPartsCost,
                batchLaborCost,
                MechanicSpecialization.GENERAL);
    }

}
