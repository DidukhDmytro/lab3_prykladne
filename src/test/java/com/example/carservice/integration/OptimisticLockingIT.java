package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    private static String orderPriorityQuery = "SELECT priority FROM repair_orders WHERE id = ?";
    private static String orderVersionQuery = "SELECT version FROM repair_orders WHERE id = ?";
    private static String staleWriterThreadName = "optimistic-stale-writer";
    private static String missingOrderMessage = "Optimistic-locking test order was not found.";
    private static String latchTimeoutMessage = "Timed out waiting for the competing transaction.";
    private static long coordinationTimeoutSeconds = 10;
    private static long threadJoinTimeoutMillis = 10000;
    private static long initialVersion = 0;
    private static long expectedVersionAfterFirstCommit = 1;
    private static long expectedVersionAfterRetry = 2;
    private static int carYear = 2024;

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;
    private RepairOrderRepository repairOrderRepository;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    OptimisticLockingIT(
            JdbcTemplate jdbcTemplate,
            jakarta.persistence.EntityManager entityManager,
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            RepairOrderRepository repairOrderRepository,
            PlatformTransactionManager transactionManager) {
        super(jdbcTemplate, entityManager);
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
        this.repairOrderRepository = repairOrderRepository;
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
        assertTrue(failure instanceof ObjectOptimisticLockingFailureException);
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
        assertTrue(conflict instanceof ObjectOptimisticLockingFailureException);

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
                    entityManager.flush();
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

    private void createOrder() {
        Customer customer = new Customer(customerId, customerName, customerPhone);
        registrationService.registerCustomer(customer);
        Car car = new Car(carVin, "Test Make", "Test Model", carYear, customer);
        registrationService.registerCar(car);
        repairOrderService.createOrder(orderId, carVin);
        entityManager.clear();
    }
}
