package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.domain.enumerate.OrderPriority;
import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumerate.WorkStatus;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.repository.RepairOrderRepository;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;

class RepairOrderPersistenceIT extends IntegrationTest {
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String CUSTOMER_NAME = "Integration Customer";
    private static final String CUSTOMER_PHONE = "555-0199";
    private static final String CAR_VIN = "INTEGRATION-VIN";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID MECHANIC_ID = UUID.randomUUID();
    private static final String ENGINE_DESCRIPTION = "Engine integration work";
    private static final String GENERAL_DESCRIPTION = "General integration work";
    private static final String ENGINE_SPECIALIZATION = "ENGINE";
    private static final String BRAKE_SPECIALIZATION = "BRAKES";
    private static final String GENERAL_SPECIALIZATION = "GENERAL";
    private static final String FORCED_PRICING_FAILURE_MESSAGE = "Forced integration-test pricing failure.";
    private static final String EXPECTED_PRICE = "390.00";
    private static final BigDecimal FIRST_PARTS = new BigDecimal("200.00");
    private static final BigDecimal FIRST_LABOR = new BigDecimal("85.00");
    private static final BigDecimal SECOND_PARTS = new BigDecimal("60.00");
    private static final BigDecimal SECOND_LABOR = new BigDecimal("45.00");
    private static final BigDecimal ZERO_COST = BigDecimal.ZERO;
    private static final BigDecimal ONE_COST = BigDecimal.ONE;
    private static final BigDecimal OVERSIZED_FINAL_PRICE = new BigDecimal("1000000000000.00");
    private static final int CAR_YEAR = 2020;
    private static final int FIRST_POSITION = 0;
    private static final int SECOND_POSITION = 1;
    private static final int EXPECTED_WORK_COUNT = 2;
    private static final Instant EXPECTED_COMPLETION_TIME = Instant.parse("2026-10-29T12:00:00Z");

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;
    private RepairOrderRepository repairOrderRepository;
    private EntityManager entityManager;
    @SpyBean
    private PricingPolicy pricingPolicy;

    @Autowired
    RepairOrderPersistenceIT(
            RegistrationService registrationService,
            RepairOrderService repairOrderService,
            RepairOrderRepository repairOrderRepository,
            EntityManager entityManager) {
        this.entityManager = entityManager;
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
        this.repairOrderRepository = repairOrderRepository;
    }

    @Test
    @DisplayName("Restores work order, mechanic, status, and priority from PostgreSQL")
    void persistsAndFetchesOrderGraph() {
        // Given
        createOrderInStatus(OrderStatus.APPROVED);
        repairOrderService.changePriority(ORDER_ID, OrderPriority.URGENT);
        entityManager.clear();

        // When
        RepairOrder loadedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();
        List<RepairOrder> approvedOrders = repairOrderRepository.findByStatus(OrderStatus.APPROVED);

        // Then
        assertEquals(OrderStatus.APPROVED, loadedOrder.getStatus());
        assertEquals(OrderPriority.URGENT, loadedOrder.getPriority());
        assertEquals(MECHANIC_ID, loadedOrder.getAssignedMechanic().getId());
        assertEquals(EXPECTED_WORK_COUNT, loadedOrder.getWorks().size());
        assertEquals(ENGINE_DESCRIPTION, loadedOrder.getWorks().get(FIRST_POSITION).getDescription());
        assertEquals(FIRST_PARTS, loadedOrder.getWorks().get(FIRST_POSITION).getPartsCost());
        assertEquals(FIRST_LABOR, loadedOrder.getWorks().get(FIRST_POSITION).getLaborCost());
        assertEquals(WorkStatus.PENDING, loadedOrder.getWorks().get(FIRST_POSITION).getStatus());
        assertEquals(GENERAL_DESCRIPTION, loadedOrder.getWorks().get(SECOND_POSITION).getDescription());
        assertEquals(WorkStatus.PENDING, loadedOrder.getWorks().get(SECOND_POSITION).getStatus());
        assertEquals(GENERAL_SPECIALIZATION, loadedOrder.getWorks()
                .get(SECOND_POSITION).getRequiredSpecialization().name());
        assertEquals(EXPECTED_WORK_COUNT, approvedOrders.get(0).getWorks().size());
        assertEquals(MECHANIC_ID, approvedOrders.get(0).getAssignedMechanic().getId());
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
        assertEquals(CUSTOMER_NAME, approvedOrders.get(0).getCar().getOwner().getFullName());
    }

    @Test
    @DisplayName("Persists completion timestamp, final price, and mechanic release")
    void completesOrderAndPersistsCompletionDetails() {
        // Given
        createOrderInStatus(OrderStatus.IN_PROGRESS);
        repairOrderService.completeWork(ORDER_ID, FIRST_POSITION);
        repairOrderService.completeWork(ORDER_ID, SECOND_POSITION);
        entityManager.clear();

        // When
        repairOrderService.complete(ORDER_ID);

        // Then
        RepairOrder completedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
        assertEquals(new BigDecimal(EXPECTED_PRICE), completedOrder.getFinalPrice());
        assertEquals(EXPECTED_COMPLETION_TIME, completedOrder.getCompletedAt());
        assertFalse(completedOrder.getAssignedMechanic().isBusy());
    }

    @ParameterizedTest(name = "completion is rolled back for unfinished={0}, pricingFailure={1}")
    @CsvSource({"true,false", "false,true"})
    @DisplayName("Rolls back failed completion")
    void rollsBackFailedCompletion(boolean leaveWorkUnfinished, boolean failPricing) {
        // Given
        createOrderInStatus(OrderStatus.IN_PROGRESS);
        if (!leaveWorkUnfinished) {
            repairOrderService.completeWork(ORDER_ID, FIRST_POSITION);
            repairOrderService.completeWork(ORDER_ID, SECOND_POSITION);
        }
        if (failPricing) {
            doThrow(new PricingException(FORCED_PRICING_FAILURE_MESSAGE))
                    .when(pricingPolicy)
                    .calculateFinalPrice(any(RepairOrder.class));
        }
        entityManager.clear();

        // When
        Runnable complete = () -> repairOrderService.complete(ORDER_ID);

        // Then
        if (leaveWorkUnfinished) {
            assertThrows(InvalidStateTransitionException.class, complete::run);
        } else {
            assertThrows(PricingException.class, complete::run);
        }
        RepairOrder persistedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();
        assertEquals(OrderStatus.IN_PROGRESS, persistedOrder.getStatus());
        assertNull(persistedOrder.getFinalPrice());
        assertTrue(persistedOrder.getAssignedMechanic().isBusy());
    }

    @Test
    @DisplayName("Rolls back completion when the database rejects the final price")
    void rollsBackCompletionWhenDatabaseRejectsFinalPrice() {
        // Given
        createOrderInStatus(OrderStatus.IN_PROGRESS);
        repairOrderService.completeWork(ORDER_ID, FIRST_POSITION);
        repairOrderService.completeWork(ORDER_ID, SECOND_POSITION);
        doReturn(OVERSIZED_FINAL_PRICE)
                .when(pricingPolicy)
                .calculateFinalPrice(any(RepairOrder.class));
        entityManager.clear();

        // When
        Runnable complete = () -> repairOrderService.complete(ORDER_ID);

        // Then
        assertThrows(DataIntegrityViolationException.class, complete::run);
        RepairOrder persistedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();
        assertEquals(OrderStatus.IN_PROGRESS, persistedOrder.getStatus());
        assertNull(persistedOrder.getFinalPrice());
        assertNull(persistedOrder.getCompletedAt());
        assertTrue(persistedOrder.getAssignedMechanic().isBusy());
    }

    @Test
    @DisplayName("Rolls back every work in a batch when its last element is incompatible")
    void rollsBackInvalidWorkBatch() {
        // Given
        createOrderInStatus(OrderStatus.APPROVED, MechanicSpecialization.ENGINE);
        List<ServiceWork> works = List.of(
                createWork(ENGINE_DESCRIPTION, ENGINE_SPECIALIZATION, FIRST_PARTS, FIRST_LABOR),
                createWork(GENERAL_DESCRIPTION, GENERAL_SPECIALIZATION, SECOND_PARTS, SECOND_LABOR),
                createWork("Brake work", BRAKE_SPECIALIZATION, ONE_COST, ZERO_COST));

        // When
        Runnable addBatch = () -> repairOrderService.addWorks(ORDER_ID, works);

        // Then
        assertThrows(MechanicNotCompatibleException.class, addBatch::run);
        assertEquals(
                EXPECTED_WORK_COUNT,
                repairOrderRepository.findById(ORDER_ID).orElseThrow().getWorks().size());
    }

    @Test
    @DisplayName("Rejects duplicate VIN in registration service")
    void rejectsDuplicateVinThroughService() {
        // Given
        Customer owner = registerCustomer(CUSTOMER_ID, CUSTOMER_NAME, CUSTOMER_PHONE);
        Car car = new Car(CAR_VIN, "Make", "Model", CAR_YEAR, owner);
        registrationService.registerCar(car);
        Car duplicate = new Car(CAR_VIN, "Other", "Model", CAR_YEAR, owner);

        // When
        Runnable registerDuplicate = () -> registrationService.registerCar(duplicate);

        // Then
        assertThrows(EntityAlreadyExistsException.class, registerDuplicate::run);
    }

    private RepairOrder createOrderInStatus(OrderStatus status) {
        return createOrderInStatus(status, MechanicSpecialization.GENERAL);
    }

    private RepairOrder createOrderInStatus(
            OrderStatus status,
            MechanicSpecialization mechanicSpecialization) {
        Customer owner = registerCustomer(CUSTOMER_ID, CUSTOMER_NAME, CUSTOMER_PHONE);
        Car car = new Car(CAR_VIN, "Make", "Model", CAR_YEAR, owner);
        registrationService.registerCar(car);
        Mechanic mechanic = new Mechanic(
                MECHANIC_ID,
                "Integration Mechanic",
                mechanicSpecialization);
        registrationService.registerMechanic(mechanic);
        repairOrderService.createOrder(ORDER_ID, CAR_VIN);
        repairOrderService.addWorks(ORDER_ID, List.of(
                createWork(ENGINE_DESCRIPTION, ENGINE_SPECIALIZATION, FIRST_PARTS, FIRST_LABOR),
                createWork(GENERAL_DESCRIPTION, GENERAL_SPECIALIZATION, SECOND_PARTS, SECOND_LABOR)));
        if (status != OrderStatus.CREATED) {
            repairOrderService.diagnose(ORDER_ID);
        }
        if (status == OrderStatus.APPROVED
                || status == OrderStatus.IN_PROGRESS
                || status == OrderStatus.COMPLETED) {
            repairOrderService.approve(ORDER_ID);
            repairOrderService.assignMechanic(ORDER_ID, MECHANIC_ID);
        }
        if (status == OrderStatus.IN_PROGRESS || status == OrderStatus.COMPLETED) {
            repairOrderService.startProgress(ORDER_ID);
        }
        if (status == OrderStatus.COMPLETED) {
            repairOrderService.completeWork(ORDER_ID, FIRST_POSITION);
            repairOrderService.completeWork(ORDER_ID, SECOND_POSITION);
            repairOrderService.complete(ORDER_ID);
        }
        return repairOrderService.findOrder(ORDER_ID).orElseThrow();
    }

    private Customer registerCustomer(UUID id, String name, String phone) {
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
