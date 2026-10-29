package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.repository.MechanicRepository;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OptimisticLockingIT extends PostgresIntegrationTest {
    private static String customerId = "optimistic-customer";
    private static String customerName = "Optimistic Test Customer";
    private static String customerPhone = "555-0200";
    private static String carVin = "OPTIMISTIC-VIN";
    private static String orderId = "optimistic-order";
    private static String firstAssignmentOrderId = "mechanic-assignment-order-one";
    private static String secondAssignmentOrderId = "mechanic-assignment-order-two";
    private static String mechanicId = "optimistic-shared-mechanic";
    private static String mechanicName = "Shared Assignment Mechanic";
    private static String workDescription = "Concurrent assignment work";
    private static String orderPriorityQuery = "SELECT priority FROM repair_orders WHERE id = ?";
    private static String orderVersionQuery = "SELECT version FROM repair_orders WHERE id = ?";
    private static String mechanicBusyQuery = "SELECT busy FROM mechanics WHERE id = ?";
    private static String mechanicVersionQuery = "SELECT version FROM mechanics WHERE id = ?";
    private static String assignedOrderCountQuery =
            "SELECT COUNT(*) FROM repair_orders WHERE mechanic_id = ?";
    private static String staleWriterThreadName = "optimistic-stale-writer";
    private static String firstAssignmentThreadName = "mechanic-assignment-writer-one";
    private static String secondAssignmentThreadName = "mechanic-assignment-writer-two";
    private static String missingOrderMessage = "Optimistic-locking test order was not found.";
    private static String latchTimeoutMessage = "Timed out waiting for the competing transaction.";
    private static String staleWriterNoFailureMessage = "No exception was thrown by the stale writer.";
    private static String exceptionMessageSeparator = ": ";
    private static long coordinationTimeoutSeconds = 10;
    private static long threadJoinTimeoutMillis = 10000;
    private static long initialVersion = 0;
    private static long expectedVersionAfterFirstCommit = 1;
    private static long expectedVersionAfterRetry = 2;
    private static long expectedMechanicVersion = 1;
    private static int competingTransactionCount = 2;
    private static int expectedCommittedAssignmentCount = 1;
    private static int expectedFailedAssignmentCount = 1;
    private static int carYear = 2024;
    private static BigDecimal workCost = BigDecimal.ZERO;

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;
    private RepairOrderRepository repairOrderRepository;
    private MechanicRepository mechanicRepository;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;
    private EntityManager entityManager;

    @Autowired
    OptimisticLockingIT(
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager,
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            RepairOrderRepository repairOrderRepository,
            MechanicRepository mechanicRepository,
            PlatformTransactionManager transactionManager) {
        super(jdbcTemplate, entityManager);
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
        this.repairOrderRepository = repairOrderRepository;
        this.mechanicRepository = mechanicRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("Detects a stale concurrent update and preserves the first commit")
    void rejectsStaleConcurrentUpdate() throws InterruptedException {
        // Given
        createOrder();
        assertEquals(
                initialVersion,
                jdbcTemplate.queryForObject(orderVersionQuery, Long.class, orderId));

        // When
        RuntimeException failure = runCompetingPriorityChanges();

        // Then
        assertTrue(
                failure instanceof ObjectOptimisticLockingFailureException,
                describeFailure(failure));
        assertEquals(
                OrderPriority.URGENT.name(),
                jdbcTemplate.queryForObject(orderPriorityQuery, String.class, orderId));
        assertEquals(
                expectedVersionAfterFirstCommit,
                jdbcTemplate.queryForObject(orderVersionQuery, Long.class, orderId));
    }

    @Test
    @DisplayName("Succeeds when the conflicting update is retried after reloading")
    void retriesStaleUpdateAfterReload() throws InterruptedException {
        // Given
        createOrder();
        RuntimeException conflict = runCompetingPriorityChanges();
        assertTrue(
                conflict instanceof ObjectOptimisticLockingFailureException,
                describeFailure(conflict));

        // When
        RepairOrder reloadedOrder = repairOrderService.getOrder(orderId);
        assertNotNull(reloadedOrder.getVersion());
        repairOrderService.changePriority(orderId, OrderPriority.NORMAL);

        // Then
        assertEquals(
                OrderPriority.NORMAL.name(),
                jdbcTemplate.queryForObject(orderPriorityQuery, String.class, orderId));
        assertEquals(
                expectedVersionAfterRetry,
                jdbcTemplate.queryForObject(orderVersionQuery, Long.class, orderId));
    }

    @Test
    @DisplayName("Rejects one of two concurrent assignments to the same mechanic")
    void rejectsConcurrentAssignmentsToSameMechanic() {
        // Given
        createOrder();
        registrationService.registerMechanic(new Mechanic(
                mechanicId,
                mechanicName,
                MechanicSpecialization.GENERAL));
        createApprovedOrder(firstAssignmentOrderId);
        createApprovedOrder(secondAssignmentOrderId);
        CountDownLatch mechanicReadsComplete = new CountDownLatch(competingTransactionCount);
        CountDownLatch assignmentsStart = new CountDownLatch(1);
        AtomicInteger committedAssignments = new AtomicInteger();
        AtomicInteger failedAssignments = new AtomicInteger();
        AtomicReference<RuntimeException> assignmentFailure = new AtomicReference<>();
        Thread firstWriter = createAssignmentThread(
                firstAssignmentThreadName,
                firstAssignmentOrderId,
                mechanicReadsComplete,
                assignmentsStart,
                committedAssignments,
                failedAssignments,
                assignmentFailure);
        Thread secondWriter = createAssignmentThread(
                secondAssignmentThreadName,
                secondAssignmentOrderId,
                mechanicReadsComplete,
                assignmentsStart,
                committedAssignments,
                failedAssignments,
                assignmentFailure);

        // When
        firstWriter.start();
        secondWriter.start();
        await(mechanicReadsComplete);
        assignmentsStart.countDown();
        join(firstWriter);
        join(secondWriter);

        // Then
        assertEquals(expectedCommittedAssignmentCount, committedAssignments.get());
        assertEquals(expectedFailedAssignmentCount, failedAssignments.get());
        assertTrue(
                assignmentFailure.get() instanceof ObjectOptimisticLockingFailureException,
                describeFailure(assignmentFailure.get()));
        assertTrue(jdbcTemplate.queryForObject(mechanicBusyQuery, Boolean.class, mechanicId));
        assertEquals(
                expectedMechanicVersion,
                jdbcTemplate.queryForObject(mechanicVersionQuery, Long.class, mechanicId));
        assertEquals(
                expectedCommittedAssignmentCount,
                jdbcTemplate.queryForObject(assignedOrderCountQuery, Integer.class, mechanicId));
    }

    private Thread createAssignmentThread(
            String threadName,
            String assignedOrderId,
            CountDownLatch mechanicReadsComplete,
            CountDownLatch assignmentsStart,
            AtomicInteger committedAssignments,
            AtomicInteger failedAssignments,
            AtomicReference<RuntimeException> assignmentFailure) {
        return new Thread(() -> {
            try {
                transactionTemplate.execute(status -> {
                    RepairOrder order = repairOrderService.findOrder(assignedOrderId)
                            .orElseThrow(() -> new IllegalStateException(missingOrderMessage));
                    Mechanic mechanic = mechanicRepository.findById(mechanicId)
                            .orElseThrow(() -> new IllegalStateException(missingOrderMessage));
                    mechanicReadsComplete.countDown();
                    await(assignmentsStart);
                    order.assignMechanic(mechanic);
                    repairOrderRepository.save(order);
                    return null;
                });
                committedAssignments.incrementAndGet();
            } catch (RuntimeException exception) {
                assignmentFailure.compareAndSet(null, exception);
                failedAssignments.incrementAndGet();
            }
        }, threadName);
    }

    private void createApprovedOrder(String assignedOrderId) {
        repairOrderService.createOrder(assignedOrderId, carVin);
        repairOrderService.addWork(
                assignedOrderId,
                new ServiceWork(
                        workDescription,
                        workCost,
                        workCost,
                        MechanicSpecialization.GENERAL));
        repairOrderService.diagnose(assignedOrderId);
        repairOrderService.approve(assignedOrderId);
    }

    private void join(Thread thread) {
        try {
            thread.join(threadJoinTimeoutMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(latchTimeoutMessage, exception);
        }
        assertFalse(thread.isAlive());
    }

    private RuntimeException runCompetingPriorityChanges() throws InterruptedException {
        CountDownLatch staleReadComplete = new CountDownLatch(1);
        CountDownLatch firstCommitComplete = new CountDownLatch(1);
        AtomicReference<RuntimeException> staleTransactionFailure = new AtomicReference<>();
        Thread staleWriter = new Thread(() -> {
            try {
                transactionTemplate.execute(status -> {
                    RepairOrder staleOrder = repairOrderRepository.findById(orderId)
                            .orElseThrow(() -> new IllegalStateException(missingOrderMessage));
                    staleReadComplete.countDown();
                    await(firstCommitComplete);
                    staleOrder.changePriority(OrderPriority.URGENT);
                    repairOrderRepository.save(staleOrder);
                    return null;
                });
            } catch (RuntimeException exception) {
                staleTransactionFailure.set(exception);
            }
        }, staleWriterThreadName);
        staleWriter.start();

        boolean staleReadCompleted =
                staleReadComplete.await(coordinationTimeoutSeconds, TimeUnit.SECONDS);
        if (!staleReadCompleted) {
            firstCommitComplete.countDown();
            staleWriter.join(threadJoinTimeoutMillis);
            throw new IllegalStateException(latchTimeoutMessage);
        }

        try {
            repairOrderService.changePriority(orderId, OrderPriority.URGENT);
        } finally {
            firstCommitComplete.countDown();
        }
        staleWriter.join(threadJoinTimeoutMillis);
        assertFalse(staleWriter.isAlive());
        return staleTransactionFailure.get();
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

    private String describeFailure(RuntimeException failure) {
        if (failure == null) {
            return staleWriterNoFailureMessage;
        }
        return failure.getClass().getName()
                + exceptionMessageSeparator
                + failure.getMessage();
    }

    private void createOrder() {
        Customer customer = new Customer(customerId, customerName, customerPhone);
        registrationService.registerCustomer(customer);
        Car car = new Car(carVin, "Test Make", "Test Model", carYear, customer);
        registrationService.registerCar(car);
        repairOrderService.createOrder(orderId, carVin);
        entityManager.clear();
    }
}
