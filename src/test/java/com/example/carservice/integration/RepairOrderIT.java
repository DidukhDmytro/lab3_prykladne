package com.example.carservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Gift;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.domain.enumerate.OrderPriority;
import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.service.RegistrationService;
import com.example.carservice.service.RepairOrderService;
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

class RepairOrderIT extends IntegrationTest {
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
    private static final String EXPECTED_NORMAL_PRICE = "390.00";
    private static final String EXPECTED_URGENT_PRICE = "416.00";
    private static final String EXPECTED_WASHER_FLUID_GIFT = "Washer fluid";
    private static final BigDecimal FIRST_PARTS = new BigDecimal("200.00");
    private static final BigDecimal FIRST_LABOR = new BigDecimal("85.00");
    private static final BigDecimal SECOND_PARTS = new BigDecimal("60.00");
    private static final BigDecimal SECOND_LABOR = new BigDecimal("45.00");
    private static final BigDecimal ZERO_COST = BigDecimal.ZERO;
    private static final BigDecimal ONE_COST = BigDecimal.ONE;
    private static final BigDecimal EXPECTED_PARTS_TOTAL = new BigDecimal("260.00");
    private static final BigDecimal EXPECTED_GIFT_MINIMUM = new BigDecimal("200");
    private static final int CAR_YEAR = 2020;
    private static final int FIRST_POSITION = 0;
    private static final int SECOND_POSITION = 1;
    private static final int EXPECTED_WORK_COUNT = 2;
    private static final int EXPECTED_STATUS_ORDER_COUNT = 1;
    private static final Instant EXPECTED_COMPLETION_TIME = Instant.parse("2026-10-29T12:00:00Z");

    private RegistrationService registrationService;
    private RepairOrderService repairOrderService;

    @SpyBean
    private PricingPolicy pricingPolicy;

    @Autowired
    RepairOrderIT(RegistrationService registrationService, RepairOrderService repairOrderService) {
        this.registrationService = registrationService;
        this.repairOrderService = repairOrderService;
    }

    @Test
    @DisplayName("Completes the repair-order lifecycle with normal pricing and a present")
    void completesFullLifecycleAndCreatesPresent() {
        // Given
        completeLifecycle(OrderPriority.NORMAL);

        // When
        RepairOrder completedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();
        List<RepairOrder> completedOrders = repairOrderService.findByStatus(OrderStatus.COMPLETED);
        Present present = repairOrderService.createPresent(ORDER_ID);

        // Then
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
        assertEquals(new BigDecimal(EXPECTED_NORMAL_PRICE), completedOrder.getFinalPrice());
        assertEquals(EXPECTED_COMPLETION_TIME, completedOrder.getCompletedAt());
        assertFalse(registrationService.findMechanic(MECHANIC_ID).orElseThrow().isBusy());
        assertEquals(EXPECTED_STATUS_ORDER_COUNT, completedOrders.size());
        assertEquals(CUSTOMER_NAME, completedOrders.get(0).getCar().getOwner().getFullName());
        assertEquals(ORDER_ID, completedOrders.get(0).getId());
        assertEquals(EXPECTED_PARTS_TOTAL, present.getPartsTotal());
        Gift gift = present.getGift().orElseThrow();
        assertEquals(EXPECTED_WASHER_FLUID_GIFT, gift.getName());
        assertEquals(EXPECTED_GIFT_MINIMUM, gift.getMinimumPartsTotal());
    }

    @Test
    @DisplayName("Includes the urgent labor surcharge in the completed order price")
    void includesUrgentSurchargeInFinalPrice() {
        // Given
        completeLifecycle(OrderPriority.URGENT);

        // When
        RepairOrder completedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();

        // Then
        assertEquals(OrderStatus.COMPLETED, completedOrder.getStatus());
        assertEquals(OrderPriority.URGENT, completedOrder.getPriority());
        assertEquals(new BigDecimal(EXPECTED_URGENT_PRICE), completedOrder.getFinalPrice());
    }

    @ParameterizedTest(name = "completion is rolled back for unfinished={0}, pricingFailure={1}")
    @CsvSource({"true,false", "false,true"})
    @DisplayName("Rolls back failed completion")
    void rollsBackFailedCompletion(boolean leaveWorkUnfinished, boolean failPricing) {
        // Given
        createInProgressOrder();
        if (!leaveWorkUnfinished) {
            repairOrderService.completeWork(ORDER_ID, FIRST_POSITION);
            repairOrderService.completeWork(ORDER_ID, SECOND_POSITION);
        }
        if (failPricing) {
            doThrow(new PricingException(FORCED_PRICING_FAILURE_MESSAGE))
                    .when(pricingPolicy)
                    .calculateFinalPrice(any(RepairOrder.class));
        }

        // When / Then
        if (leaveWorkUnfinished) {
            assertThrows(InvalidStateTransitionException.class, () -> repairOrderService.complete(ORDER_ID));
        } else {
            assertThrows(PricingException.class, () -> repairOrderService.complete(ORDER_ID));
        }
        RepairOrder persistedOrder = repairOrderService.findOrder(ORDER_ID).orElseThrow();
        assertEquals(OrderStatus.IN_PROGRESS, persistedOrder.getStatus());
        assertNull(persistedOrder.getFinalPrice());
        assertTrue(registrationService.findMechanic(MECHANIC_ID).orElseThrow().isBusy());
    }

    @Test
    @DisplayName("Rolls back every work in a batch when its last element is incompatible")
    void rollsBackInvalidWorkBatch() {
        // Given
        createApprovedOrder(MechanicSpecialization.ENGINE);
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
                repairOrderService.findOrder(ORDER_ID).orElseThrow().getWorks().size());
    }

    @Test
    @DisplayName("Rejects duplicate VIN in registration service")
    void rejectsDuplicateVinThroughService() {
        // Given
        Customer owner = registerCustomer();
        registrationService.registerCar(new Car(CAR_VIN, "Make", "Model", CAR_YEAR, owner));
        Car duplicate = new Car(CAR_VIN, "Other", "Model", CAR_YEAR, owner);

        // When / Then
        assertThrows(EntityAlreadyExistsException.class, () -> registrationService.registerCar(duplicate));
    }

    private void completeLifecycle(OrderPriority priority) {
        Customer owner = registerCustomer();
        registrationService.registerCar(new Car(CAR_VIN, "Make", "Model", CAR_YEAR, owner));
        registrationService.registerMechanic(new Mechanic(
                MECHANIC_ID,
                "Integration Mechanic",
                MechanicSpecialization.GENERAL));
        repairOrderService.createOrder(ORDER_ID, CAR_VIN);
        repairOrderService.changePriority(ORDER_ID, priority);
        repairOrderService.addWorks(ORDER_ID, List.of(
                createWork(ENGINE_DESCRIPTION, ENGINE_SPECIALIZATION, FIRST_PARTS, FIRST_LABOR),
                createWork(GENERAL_DESCRIPTION, GENERAL_SPECIALIZATION, SECOND_PARTS, SECOND_LABOR)));
        repairOrderService.diagnose(ORDER_ID);
        repairOrderService.approve(ORDER_ID);
        repairOrderService.assignMechanic(ORDER_ID, MECHANIC_ID);
        repairOrderService.startProgress(ORDER_ID);
        repairOrderService.completeWork(ORDER_ID, FIRST_POSITION);
        repairOrderService.completeWork(ORDER_ID, SECOND_POSITION);
        repairOrderService.complete(ORDER_ID);
    }

    private void createInProgressOrder() {
        createApprovedOrder(MechanicSpecialization.GENERAL);
        repairOrderService.startProgress(ORDER_ID);
    }

    private void createApprovedOrder(MechanicSpecialization mechanicSpecialization) {
        Customer owner = registerCustomer();
        registrationService.registerCar(new Car(CAR_VIN, "Make", "Model", CAR_YEAR, owner));
        registrationService.registerMechanic(new Mechanic(
                MECHANIC_ID,
                "Integration Mechanic",
                mechanicSpecialization));
        repairOrderService.createOrder(ORDER_ID, CAR_VIN);
        repairOrderService.addWorks(ORDER_ID, List.of(
                createWork(ENGINE_DESCRIPTION, ENGINE_SPECIALIZATION, FIRST_PARTS, FIRST_LABOR),
                createWork(GENERAL_DESCRIPTION, GENERAL_SPECIALIZATION, SECOND_PARTS, SECOND_LABOR)));
        repairOrderService.diagnose(ORDER_ID);
        repairOrderService.approve(ORDER_ID);
        repairOrderService.assignMechanic(ORDER_ID, MECHANIC_ID);
    }

    private Customer registerCustomer() {
        return registrationService.registerCustomer(
                new Customer(CUSTOMER_ID, CUSTOMER_NAME, CUSTOMER_PHONE));
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
