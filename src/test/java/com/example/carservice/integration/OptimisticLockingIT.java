package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.domain.enumerate.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.exception.MechanicAlreadyAssignedException;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OptimisticLockingIT extends IntegrationTest {
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String CUSTOMER_NAME = "Optimistic Test Customer";
    private static final String CUSTOMER_PHONE = "555-0200";
    private static final String CAR_VIN = "OPTIMISTIC-VIN";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID FIRST_ASSIGNMENT_ORDER_ID = UUID.randomUUID();
    private static final UUID SECOND_ASSIGNMENT_ORDER_ID = UUID.randomUUID();
    private static final UUID MECHANIC_ID = UUID.randomUUID();
    private static final String MECHANIC_NAME = "Shared Assignment Mechanic";
    private static final String WORK_DESCRIPTION = "Concurrent assignment work";
    private static final String MISSING_ORDER_MESSAGE = "Optimistic-locking test order was not found.";
    private static final String LATCH_TIMEOUT_MESSAGE = "Timed out waiting for the competing transaction.";
    private static final String NO_FAILURE_MESSAGE = "No exception was thrown by the competing operation.";
    private static final String EXCEPTION_MESSAGE_SEPARATOR = ": ";
    private static final long COORDINATION_TIMEOUT_SECONDS = 10;
    private static final long INITIAL_VERSION = 0;
    private static final long EXPECTED_VERSION_AFTER_FIRST_COMMIT = 1;
    private static final long EXPECTED_VERSION_AFTER_RETRY = 2;
    private static final int COMPETING_TRANSACTION_COUNT = 2;
    private static final int EXPECTED_COMMITTED_ASSIGNMENT_COUNT = 1;
    private static final int EXPECTED_FAILED_ASSIGNMENT_COUNT = 1;
    private static final int EXPECTED_ASSIGNED_ORDER_COUNT = 1;
    private static final int CAR_YEAR = 2024;
    private static final BigDecimal WORK_COST = BigDecimal.ZERO;

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;
    private RepairOrderRepository repairOrderRepository;
    private TransactionTemplate transactionTemplate;

    @Autowired
    OptimisticLockingIT(
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            RepairOrderRepository repairOrderRepository,
            PlatformTransactionManager transactionManager) {
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
        this.repairOrderRepository = repairOrderRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("Detects a stale concurrent update and preserves the first commit")
    void rejectsStaleConcurrentUpdate() {
        // Given
        createOrder();
        ExecutorService executor = Executors.newFixedThreadPool(COMPETING_TRANSACTION_COUNT);
        try {
            assertEquals(INITIAL_VERSION, repairOrderRepository.findById(ORDER_ID).orElseThrow().getVersion());

            // When
            RuntimeException failure = runCompetingPriorityChanges(executor);

            // Then
            assertTrue(
                    failure instanceof ObjectOptimisticLockingFailureException,
                    describeFailure(failure));
            RepairOrder persistedOrder = repairOrderRepository.findById(ORDER_ID).orElseThrow();
            assertEquals(OrderPriority.URGENT, persistedOrder.getPriority());
            assertEquals(EXPECTED_VERSION_AFTER_FIRST_COMMIT, persistedOrder.getVersion());
        } finally {
            shutdownExecutor(executor);
        }
    }

    @Test
    @DisplayName("Succeeds when the conflicting update is retried after reloading")
    void retriesStaleUpdateAfterReload() {
        // Given
        createOrder();
        ExecutorService executor = Executors.newFixedThreadPool(COMPETING_TRANSACTION_COUNT);
        try {
            RuntimeException conflict = runCompetingPriorityChanges(executor);
            assertTrue(
                    conflict instanceof ObjectOptimisticLockingFailureException,
                    describeFailure(conflict));

            // When
            RepairOrder reloadedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();
            assertNotNull(reloadedOrder.getVersion());
            repairOrderService.changePriority(ORDER_ID, OrderPriority.NORMAL);

            // Then
            RepairOrder persistedOrder = repairOrderRepository.findById(ORDER_ID).orElseThrow();
            assertEquals(OrderPriority.NORMAL, persistedOrder.getPriority());
            assertEquals(EXPECTED_VERSION_AFTER_RETRY, persistedOrder.getVersion());
        } finally {
            shutdownExecutor(executor);
        }
    }

    @Test
    @DisplayName("Rejects one of two concurrent assignments to the same mechanic")
    void rejectsConcurrentAssignmentsToSameMechanic() {
        // Given
        registerCustomerAndCar();
        registrationService.registerMechanic(new Mechanic(
                MECHANIC_ID,
                MECHANIC_NAME,
                MechanicSpecialization.GENERAL));
        createApprovedOrder(FIRST_ASSIGNMENT_ORDER_ID);
        createApprovedOrder(SECOND_ASSIGNMENT_ORDER_ID);
        ExecutorService executor = Executors.newFixedThreadPool(COMPETING_TRANSACTION_COUNT);
        CountDownLatch assignmentsStart = new CountDownLatch(1);
        try {
            CompletableFuture<RuntimeException> firstAssignment = createAssignmentFuture(
                    FIRST_ASSIGNMENT_ORDER_ID,
                    assignmentsStart,
                    executor);
            CompletableFuture<RuntimeException> secondAssignment = createAssignmentFuture(
                    SECOND_ASSIGNMENT_ORDER_ID,
                    assignmentsStart,
                    executor);

            // When
            assignmentsStart.countDown();
            RuntimeException firstFailure = awaitResult(firstAssignment);
            RuntimeException secondFailure = awaitResult(secondAssignment);

            // Then
            int committedAssignments = (firstFailure == null ? 1 : 0) + (secondFailure == null ? 1 : 0);
            assertEquals(EXPECTED_COMMITTED_ASSIGNMENT_COUNT, committedAssignments);
            int failedAssignments = (firstFailure == null ? 0 : 1) + (secondFailure == null ? 0 : 1);
            assertEquals(EXPECTED_FAILED_ASSIGNMENT_COUNT, failedAssignments);
            RuntimeException assignmentFailure = firstFailure == null ? secondFailure : firstFailure;
            assertTrue(
                    assignmentFailure instanceof ObjectOptimisticLockingFailureException
                            || assignmentFailure instanceof MechanicAlreadyAssignedException,
                    describeFailure(assignmentFailure));
            Mechanic persistedMechanic = registrationService.findMechanic(MECHANIC_ID).orElseThrow();
            assertTrue(persistedMechanic.isBusy());
            long assignedOrderCount = List.of(FIRST_ASSIGNMENT_ORDER_ID, SECOND_ASSIGNMENT_ORDER_ID).stream()
                    .map(orderId -> repairOrderService.findOrder(orderId).orElseThrow())
                    .filter(order -> order.getAssignedMechanic() != null)
                    .filter(order -> MECHANIC_ID.equals(order.getAssignedMechanic().getId()))
                    .count();
            assertEquals(EXPECTED_ASSIGNED_ORDER_COUNT, assignedOrderCount);
        } finally {
            assignmentsStart.countDown();
            shutdownExecutor(executor);
        }
    }

    private CompletableFuture<RuntimeException> createAssignmentFuture(
            UUID assignedOrderId,
            CountDownLatch assignmentsStart,
            ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                await(assignmentsStart);
                repairOrderService.assignMechanic(assignedOrderId, MECHANIC_ID);
                return null;
            } catch (RuntimeException exception) {
                return exception;
            }
        }, executor);
    }

    private void createApprovedOrder(UUID assignedOrderId) {
        repairOrderService.createOrder(assignedOrderId, CAR_VIN);
        repairOrderService.addWork(
                assignedOrderId,
                new ServiceWork(
                        WORK_DESCRIPTION,
                        WORK_COST,
                        WORK_COST,
                        MechanicSpecialization.GENERAL));
        repairOrderService.diagnose(assignedOrderId);
        repairOrderService.approve(assignedOrderId);
    }

    private RuntimeException runCompetingPriorityChanges(ExecutorService executor) {
        CountDownLatch staleReadComplete = new CountDownLatch(1);
        CountDownLatch firstCommitComplete = new CountDownLatch(1);
        CompletableFuture<RuntimeException> staleWriter = CompletableFuture.supplyAsync(() -> {
            try {
                // Keep this transaction open so its read stays stale after the competing commit.
                transactionTemplate.execute(status -> {
                    RepairOrder staleOrder = repairOrderRepository.findById(ORDER_ID)
                            .orElseThrow(() -> new IllegalStateException(MISSING_ORDER_MESSAGE));
                    staleReadComplete.countDown();
                    await(firstCommitComplete);
                    staleOrder.changePriority(OrderPriority.URGENT);
                    repairOrderRepository.save(staleOrder);
                    return null;
                });
                return null;
            } catch (RuntimeException exception) {
                return exception;
            }
        }, executor);
        await(staleReadComplete);

        try {
            repairOrderService.changePriority(ORDER_ID, OrderPriority.URGENT);
        } finally {
            firstCommitComplete.countDown();
        }
        return awaitResult(staleWriter);
    }

    private void await(CountDownLatch latch) {
        try {
            boolean released = latch.await(COORDINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!released) {
                throw new IllegalStateException(LATCH_TIMEOUT_MESSAGE);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(LATCH_TIMEOUT_MESSAGE, exception);
        }
    }

    private String describeFailure(RuntimeException failure) {
        if (failure == null) {
            return NO_FAILURE_MESSAGE;
        }
        return failure.getClass().getName()
                + EXCEPTION_MESSAGE_SEPARATOR
                + failure.getMessage();
    }

    private <T> T awaitResult(CompletableFuture<T> future) {
        try {
            return future.get(COORDINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(LATCH_TIMEOUT_MESSAGE, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException(LATCH_TIMEOUT_MESSAGE, exception);
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(COORDINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(COORDINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(LATCH_TIMEOUT_MESSAGE);
                }
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException(LATCH_TIMEOUT_MESSAGE, exception);
        }
    }

    private void createOrder() {
        registerCustomerAndCar();
        repairOrderService.createOrder(ORDER_ID, CAR_VIN);
    }

    private void registerCustomerAndCar() {
        Customer customer = new Customer(CUSTOMER_ID, CUSTOMER_NAME, CUSTOMER_PHONE);
        registrationService.registerCustomer(customer);
        Car car = new Car(CAR_VIN, "Test Make", "Test Model", CAR_YEAR, customer);
        registrationService.registerCar(car);
    }
}
