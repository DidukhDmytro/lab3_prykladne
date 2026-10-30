package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.WorkStatus;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.integration.PostgresIntegrationTestConfiguration.AdjustablePricingPolicy;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class RepairOrderPersistenceIT extends PostgresIntegrationTest {
    private static String customerId = "integration-customer";
    private static String customerName = "Integration Customer";
    private static String customerPhone = "555-0199";
    private static String carVin = "INTEGRATION-VIN";
    private static String orderId = "integration-order";
    private static String mechanicId = "integration-mechanic";
    private static String engineDescription = "Engine integration work";
    private static String generalDescription = "General integration work";
    private static String engineSpecialization = "ENGINE";
    private static String brakeSpecialization = "BRAKES";
    private static String generalSpecialization = "GENERAL";
    private static String pendingStatusSql = "PENDING";
    private static String completedStatusSql = "COMPLETED";
    private static String inProgressStatusSql = "IN_PROGRESS";
    private static String duplicateVinSql = "INSERT INTO cars (vin, year, owner_id) VALUES (?, ?, ?)";
    private static String serviceWorkInsertSql =
            "INSERT INTO service_works (order_id, position, description, parts_cost, labor_cost, required_specialization, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static String carInsertSql =
            "INSERT INTO cars (vin, year, owner_id) VALUES (?, ?, ?)";
    private static String priceColumnQuery = "SELECT final_price FROM repair_orders WHERE id = ?";
    private static String statusColumnQuery = "SELECT status FROM repair_orders WHERE id = ?";
    private static String mechanicBusyQuery =
            "SELECT busy FROM mechanics WHERE id = ?";
    private static String completedAtQuery =
            "SELECT completed_at FROM repair_orders WHERE id = ?";
    private static String workCountQuery =
            "SELECT COUNT(*) FROM service_works WHERE order_id = ?";
    private static String missingOwnerId = "nonexistent-owner";
    private static String newVin = "NEW-INTEGRATION-VIN";
    private static String expectedPrice = "390.00";
    private static BigDecimal firstParts = new BigDecimal("200.00");
    private static BigDecimal firstLabor = new BigDecimal("85.00");
    private static BigDecimal secondParts = new BigDecimal("60.00");
    private static BigDecimal secondLabor = new BigDecimal("45.00");
    private static BigDecimal zeroCost = BigDecimal.ZERO;
    private static BigDecimal oneCost = BigDecimal.ONE;
    private static BigDecimal oversizedFinalPrice = new BigDecimal("1000000000000.00");
    private static int oversizedDescriptionLength = 300;
    private static int carYear = 2020;
    private static int firstPosition = 0;
    private static int secondPosition = 1;
    private static int expectedWorkCount = 2;
    private static int workBatchSize = 3;
    private static Instant expectedCompletionTime = Instant.parse("2026-10-29T12:00:00Z");

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;
    private RepairOrderRepository repairOrderRepository;
    private JdbcTemplate jdbcTemplate;
    private AdjustablePricingPolicy adjustablePricingPolicy;
    private EntityManager entityManager;

    @Autowired
    RepairOrderPersistenceIT(
            JdbcTemplate jdbcTemplate,
            EntityManager entityManager,
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            RepairOrderRepository repairOrderRepository,
            AdjustablePricingPolicy adjustablePricingPolicy) {
        super(jdbcTemplate, entityManager);
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
        this.repairOrderRepository = repairOrderRepository;
        this.adjustablePricingPolicy = adjustablePricingPolicy;
    }

    @BeforeEach
    void resetPricingPolicy() {
        adjustablePricingPolicy.setFailCalculation(false);
        adjustablePricingPolicy.setFinalPriceOverride(null);
    }

    @Test
    @DisplayName("Restores work order, mechanic, status, and priority from PostgreSQL")
    void persistsAndFetchesOrderGraph() {
        // Given
        createOrderInStatus(OrderStatus.APPROVED);
        repairOrderService.changePriority(orderId, OrderPriority.URGENT);
        entityManager.clear();

        // When
        RepairOrder loadedOrder = repairOrderService.getOrder(orderId);
        List<RepairOrder> approvedOrders = repairOrderRepository.findByStatus(OrderStatus.APPROVED);

        // Then
        assertEquals(OrderStatus.APPROVED, loadedOrder.getStatus());
        assertEquals(OrderPriority.URGENT, loadedOrder.getPriority());
        assertEquals(mechanicId, loadedOrder.getAssignedMechanic().getId());
        assertEquals(expectedWorkCount, loadedOrder.getWorks().size());
        assertEquals(engineDescription, loadedOrder.getWorks().get(firstPosition).getDescription());
        assertEquals(firstParts, loadedOrder.getWorks().get(firstPosition).getPartsCost());
        assertEquals(firstLabor, loadedOrder.getWorks().get(firstPosition).getLaborCost());
        assertEquals(WorkStatus.PENDING, loadedOrder.getWorks().get(firstPosition).getStatus());
        assertEquals(generalDescription, loadedOrder.getWorks().get(secondPosition).getDescription());
        assertEquals(WorkStatus.PENDING, loadedOrder.getWorks().get(secondPosition).getStatus());
        assertEquals(generalSpecialization, loadedOrder.getWorks()
                .get(secondPosition).getRequiredSpecialization().name());
        assertEquals(expectedWorkCount, approvedOrders.get(0).getWorks().size());
        assertEquals(mechanicId, approvedOrders.get(0).getAssignedMechanic().getId());
    }

    @Test
    @DisplayName("Loads the car owner with orders returned by status lookup")
    void loadsCarOwnerOutsideTransactionForStatusLookup() {
        // Given
        createOrderInStatus(OrderStatus.APPROVED);
        entityManager.clear();

        // When
        List<RepairOrder> approvedOrders = repairOrderRepository.findByStatus(OrderStatus.APPROVED);

        // Then
        assertEquals(customerName, approvedOrders.get(0).getCar().getOwner().getFullName());
    }

    @Test
    @DisplayName("Persists completion timestamp, final price, and mechanic release")
    void completesOrderAndPersistsCompletionDetails() {
        // Given
        createOrderInStatus(OrderStatus.IN_PROGRESS);
        repairOrderService.completeWork(orderId, firstPosition);
        repairOrderService.completeWork(orderId, secondPosition);
        entityManager.clear();

        // When
        repairOrderService.complete(orderId);

        // Then
        assertEquals(completedStatusSql, jdbcTemplate.queryForObject(statusColumnQuery, String.class, orderId));
        assertEquals(
                new BigDecimal(expectedPrice),
                jdbcTemplate.queryForObject(priceColumnQuery, BigDecimal.class, orderId));
        assertEquals(
                expectedCompletionTime,
                jdbcTemplate.queryForObject(completedAtQuery, java.time.OffsetDateTime.class, orderId)
                        .toInstant());
        assertFalse(jdbcTemplate.queryForObject(mechanicBusyQuery, Boolean.class, mechanicId));
    }

    @ParameterizedTest(name = "completion is rolled back for unfinished={0}, pricingFailure={1}")
    @CsvSource({"true,false", "false,true"})
    @DisplayName("Rolls back failed completion")
    void rollsBackFailedCompletion(boolean leaveWorkUnfinished, boolean failPricing) {
        // Given
        createOrderInStatus(OrderStatus.IN_PROGRESS);
        if (!leaveWorkUnfinished) {
            repairOrderService.completeWork(orderId, firstPosition);
            repairOrderService.completeWork(orderId, secondPosition);
        }
        adjustablePricingPolicy.setFailCalculation(failPricing);
        entityManager.clear();

        // When
        Runnable complete = () -> repairOrderService.complete(orderId);

        // Then
        if (leaveWorkUnfinished) {
            assertThrows(InvalidStateTransitionException.class, complete::run);
        } else {
            assertThrows(com.example.carservice.pricing.PricingException.class, complete::run);
        }
        assertEquals(inProgressStatusSql, jdbcTemplate.queryForObject(statusColumnQuery, String.class, orderId));
        assertNull(jdbcTemplate.queryForObject(priceColumnQuery, BigDecimal.class, orderId));
        assertTrue(jdbcTemplate.queryForObject(mechanicBusyQuery, Boolean.class, mechanicId));
    }

    @Test
    @DisplayName("Persists a successful work batch")
    void persistsWorkBatch() {
        // Given
        createOrderInStatus(OrderStatus.CREATED);
        List<ServiceWork> works = List.of(
                createWork(engineDescription, engineSpecialization, firstParts, firstLabor),
                createWork(generalDescription, generalSpecialization, secondParts, secondLabor),
                createWork(generalDescription, generalSpecialization, oneCost, zeroCost));

        // When
        repairOrderService.addWorks(orderId, works);

        // Then
        assertEquals(
                expectedWorkCount + workBatchSize,
                jdbcTemplate.queryForObject(workCountQuery, Integer.class, orderId));
    }

    @Test
    @DisplayName("Rolls back a work batch when the database rejects one element")
    void rollsBackWorkBatchWhenDatabaseRejectsElement() {
        // Given
        createOrderInStatus(OrderStatus.CREATED);
        String oversizedDescription = "x".repeat(oversizedDescriptionLength);
        List<ServiceWork> works = List.of(
                createWork(engineDescription, engineSpecialization, firstParts, firstLabor),
                createWork(generalDescription, generalSpecialization, secondParts, secondLabor),
                createWork(oversizedDescription, generalSpecialization, oneCost, zeroCost));

        // When
        Runnable addBatch = () -> repairOrderService.addWorks(orderId, works);

        // Then
        assertThrows(DataIntegrityViolationException.class, addBatch::run);
        assertEquals(expectedWorkCount, jdbcTemplate.queryForObject(workCountQuery, Integer.class, orderId));
    }

    @Test
    @DisplayName("Rolls back completion when the database rejects the final price")
    void rollsBackCompletionWhenDatabaseRejectsFinalPrice() {
        // Given
        createOrderInStatus(OrderStatus.IN_PROGRESS);
        repairOrderService.completeWork(orderId, firstPosition);
        repairOrderService.completeWork(orderId, secondPosition);
        adjustablePricingPolicy.setFinalPriceOverride(oversizedFinalPrice);
        entityManager.clear();

        // When
        Runnable complete = () -> repairOrderService.complete(orderId);

        // Then
        assertThrows(DataIntegrityViolationException.class, complete::run);
        assertEquals(inProgressStatusSql, jdbcTemplate.queryForObject(statusColumnQuery, String.class, orderId));
        assertNull(jdbcTemplate.queryForObject(priceColumnQuery, BigDecimal.class, orderId));
        assertNull(jdbcTemplate.queryForObject(completedAtQuery, java.time.OffsetDateTime.class, orderId));
        assertTrue(jdbcTemplate.queryForObject(mechanicBusyQuery, Boolean.class, mechanicId));
    }

    @Test
    @DisplayName("Rolls back every work in a batch when its last element is incompatible")
    void rollsBackInvalidWorkBatch() {
        // Given
        createOrderInStatus(OrderStatus.APPROVED, MechanicSpecialization.ENGINE);
        List<ServiceWork> works = List.of(
                createWork(engineDescription, engineSpecialization, firstParts, firstLabor),
                createWork(generalDescription, generalSpecialization, secondParts, secondLabor),
                createWork("Brake work", brakeSpecialization, oneCost, zeroCost));

        // When
        Runnable addBatch = () -> repairOrderService.addWorks(orderId, works);

        // Then
        assertThrows(MechanicNotCompatibleException.class, addBatch::run);
        assertEquals(expectedWorkCount, jdbcTemplate.queryForObject(workCountQuery, Integer.class, orderId));
    }

    @Test
    @DisplayName("Rejects duplicate VIN in registration service")
    void rejectsDuplicateVinThroughService() {
        // Given
        Customer owner = registerCustomer(customerId, customerName, customerPhone);
        Car car = new Car(carVin, "Make", "Model", carYear, owner);
        registrationService.registerCar(car);
        Car duplicate = new Car(carVin, "Other", "Model", carYear, owner);

        // When
        Runnable registerDuplicate = () -> registrationService.registerCar(duplicate);

        // Then
        assertThrows(EntityAlreadyExistsException.class, registerDuplicate::run);
    }

    @Test
    @DisplayName("Rejects duplicate VIN through the database primary key")
    void rejectsDuplicateVinThroughDatabaseConstraint() {
        // Given
        Customer owner = registerCustomer(customerId, customerName, customerPhone);
        registrationService.registerCar(new Car(carVin, "Make", "Model", carYear, owner));

        // When
        Runnable insertDuplicate = () -> jdbcTemplate.update(duplicateVinSql, carVin, carYear, customerId);

        // Then
        assertThrows(DataIntegrityViolationException.class, insertDuplicate::run);
    }

    @ParameterizedTest(name = "parts={0}, labor={1} violates the non-negative cost check")
    @CsvSource({"-1.00,1.00", "1.00,-1.00"})
    @DisplayName("Rejects negative service-work costs in PostgreSQL")
    void rejectsNegativeCosts(BigDecimal partsCost, BigDecimal laborCost) {
        // Given
        createOrderInStatus(OrderStatus.CREATED);

        // When
        Runnable insertNegativeCost = () -> jdbcTemplate.update(
                serviceWorkInsertSql,
                orderId,
                firstPosition,
                engineDescription,
                partsCost,
                laborCost,
                engineSpecialization,
                pendingStatusSql);

        // Then
        assertThrows(DataIntegrityViolationException.class, insertNegativeCost::run);
    }

    @Test
    @DisplayName("Rejects a car with a missing owner in PostgreSQL")
    void rejectsCarWithMissingOwner() {
        // Given
        String vin = newVin;

        // When
        Runnable insertCar = () -> jdbcTemplate.update(carInsertSql, vin, carYear, missingOwnerId);

        // Then
        assertThrows(DataIntegrityViolationException.class, insertCar::run);
    }

    private RepairOrder createOrderInStatus(OrderStatus status) {
        return createOrderInStatus(status, MechanicSpecialization.GENERAL);
    }

    private RepairOrder createOrderInStatus(
            OrderStatus status,
            MechanicSpecialization mechanicSpecialization) {
        Customer owner = registerCustomer(customerId, customerName, customerPhone);
        Car car = new Car(carVin, "Make", "Model", carYear, owner);
        registrationService.registerCar(car);
        Mechanic mechanic = new Mechanic(
                mechanicId,
                "Integration Mechanic",
                mechanicSpecialization);
        registrationService.registerMechanic(mechanic);
        repairOrderService.createOrder(orderId, carVin);
        repairOrderService.addWorks(orderId, List.of(
                createWork(engineDescription, engineSpecialization, firstParts, firstLabor),
                createWork(generalDescription, generalSpecialization, secondParts, secondLabor)));
        if (status != OrderStatus.CREATED) {
            repairOrderService.diagnose(orderId);
        }
        if (status == OrderStatus.APPROVED
                || status == OrderStatus.IN_PROGRESS
                || status == OrderStatus.COMPLETED) {
            repairOrderService.approve(orderId);
            repairOrderService.assignMechanic(orderId, mechanicId);
        }
        if (status == OrderStatus.IN_PROGRESS || status == OrderStatus.COMPLETED) {
            repairOrderService.startProgress(orderId);
        }
        if (status == OrderStatus.COMPLETED) {
            repairOrderService.completeWork(orderId, firstPosition);
            repairOrderService.completeWork(orderId, secondPosition);
            repairOrderService.complete(orderId);
        }
        return repairOrderService.getOrder(orderId);
    }

    private Customer registerCustomer(String id, String name, String phone) {
        Customer customer = new Customer(id, name, phone);
        return registrationService.registerCustomer(customer);
    }

    private ServiceWork createWork(
            String description,
            String specialization,
            BigDecimal partsCost,
            BigDecimal laborCost) {
        return new ServiceWork(
                description,
                partsCost,
                laborCost,
                MechanicSpecialization.valueOf(specialization));
    }
}
