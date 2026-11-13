package com.example.carservice.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.domain.enumerate.OrderPriority;
import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.domain.enumerate.WorkStatus;
import com.example.carservice.exception.DomainException;
import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DomainInvariantTest {
    private static final BigDecimal COMPLETION_PRICE = new BigDecimal("125.00");
    private static final Instant COMPLETION_TIME = Instant.parse("2026-10-29T12:00:00Z");

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String CUSTOMER_NAME = "Domain Customer";
    private static final String PHONE_NUMBER = "555-0100";
    private static final String VIN = "VIN-DOMAIN";
    private static final String WORK_DESCRIPTION = "Domain work";
    private Customer customer;
    private Car car;

    @BeforeEach
    void setUp() {
        customer = new Customer(CUSTOMER_ID, CUSTOMER_NAME, PHONE_NUMBER);
        car = new Car(VIN, "Test", "Model", 2020, customer);
    }

    @Test
    @DisplayName("Validates customer constructor arguments")
    void validatesCustomerConstructor() {
        // Given
        String blankName = " ";
        String blankPhone = " ";

        // When
        ExecutableCalls invalidCustomers = new ExecutableCalls();
        invalidCustomers.add(() -> new Customer(null, CUSTOMER_NAME, PHONE_NUMBER));
        invalidCustomers.add(() -> new Customer(CUSTOMER_ID, null, PHONE_NUMBER));
        invalidCustomers.add(() -> new Customer(CUSTOMER_ID, blankName, PHONE_NUMBER));
        invalidCustomers.add(() -> new Customer(CUSTOMER_ID, CUSTOMER_NAME, null));
        invalidCustomers.add(() -> new Customer(CUSTOMER_ID, CUSTOMER_NAME, blankPhone));

        // Then
        invalidCustomers.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Validates car constructor arguments")
    void validatesCarConstructor() {
        // Given
        String blankVin = " ";

        // When
        ExecutableCalls invalidCars = new ExecutableCalls();
        invalidCars.add(() -> new Car(null, "Test", "Model", 2020, customer));
        invalidCars.add(() -> new Car(blankVin, "Test", "Model", 2020, customer));
        invalidCars.add(() -> new Car(VIN, "Test", "Model", 2020, null));

        // Then
        invalidCars.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Validates mechanic constructor arguments")
    void validatesMechanicConstructor() {
        // Given
        String blankName = " ";

        // When
        ExecutableCalls invalidMechanics = new ExecutableCalls();
        invalidMechanics.add(() ->
                new Mechanic(null, CUSTOMER_NAME, MechanicSpecialization.GENERAL));
        invalidMechanics.add(() ->
                new Mechanic(UUID.randomUUID(), null, MechanicSpecialization.GENERAL));
        invalidMechanics.add(() ->
                new Mechanic(UUID.randomUUID(), blankName, MechanicSpecialization.GENERAL));
        invalidMechanics.add(() ->
                new Mechanic(UUID.randomUUID(), CUSTOMER_NAME, null));

        // Then
        invalidMechanics.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Validates repair order constructor arguments")
    void validatesRepairOrderConstructor() {
        // Given
        // When
        ExecutableCalls invalidOrders = new ExecutableCalls();
        invalidOrders.add(() -> new RepairOrder(null, car));
        invalidOrders.add(() -> new RepairOrder(UUID.randomUUID(), null));

        // Then
        invalidOrders.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Allows zero service work costs")
    void allowsZeroCosts() {
        // Given
        BigDecimal zeroCost = BigDecimal.ZERO;

        // When
        Runnable constructZeroCostWork = () ->
                new ServiceWork(WORK_DESCRIPTION, zeroCost, zeroCost, MechanicSpecialization.GENERAL);

        // Then
        assertDoesNotThrow(constructZeroCostWork::run);
    }

    @Test
    @DisplayName("Rejects adding null service work")
    void rejectsNullServiceWork() {
        // Given
        RepairOrder order = new RepairOrder(UUID.randomUUID(), car);

        // When
        Runnable addNullWork = () -> order.addWork(null);

        // Then
        assertThrows(InvalidArgumentException.class, addNullWork::run);
    }

    @Test
    @DisplayName("Rejects assigning a null mechanic")
    void rejectsNullMechanicAssignment() {
        // Given
        RepairOrder order = new RepairOrder(UUID.randomUUID(), car);

        // When
        Runnable assignNullMechanic = () -> order.assignMechanic(null);

        // Then
        assertThrows(InvalidArgumentException.class, assignNullMechanic::run);
    }

    @Test
    @DisplayName("Does not add incompatible work after mechanic assignment")
    void rejectsIncompatibleWorkAfterMechanicAssignment() {
        // Given
        RepairOrder order = new RepairOrder(UUID.randomUUID(), car);
        Mechanic mechanic =
                new Mechanic(UUID.randomUUID(), CUSTOMER_NAME, MechanicSpecialization.ENGINE);
        ServiceWork incompatibleWork = createWork(MechanicSpecialization.BRAKES);
        order.assignMechanic(mechanic);

        // When
        Runnable addIncompatibleWork = () -> order.addWork(incompatibleWork);

        // Then
        assertThrows(MechanicNotCompatibleException.class, addIncompatibleWork::run);
        assertTrue(order.getWorks().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "ENGINE,ENGINE",
            "GENERAL,ENGINE",
            "ENGINE,GENERAL",
            "GENERAL,GENERAL"
    })
    @DisplayName("Allows compatible work after mechanic assignment")
    void allowsCompatibleWorkAfterMechanicAssignment(
            MechanicSpecialization mechanicSpecialization,
            MechanicSpecialization workSpecialization) {
        // Given
        RepairOrder order = new RepairOrder(UUID.randomUUID(), car);
        Mechanic mechanic =
                new Mechanic(UUID.randomUUID(), CUSTOMER_NAME, mechanicSpecialization);
        ServiceWork compatibleWork = createWork(workSpecialization);
        order.assignMechanic(mechanic);

        // When
        order.addWork(compatibleWork);

        // Then
        assertEquals(1, order.getWorks().size());
        assertEquals(compatibleWork, order.getWorks().get(0));
    }

    @Test
    @DisplayName("Rejects null and negative service work costs")
    void rejectsInvalidCosts() {
        // Given
        BigDecimal negativeCost = new BigDecimal("-0.01");

        // When
        ExecutableCalls invalidWorks = new ExecutableCalls();
        invalidWorks.add(() ->
                new ServiceWork(WORK_DESCRIPTION, null, BigDecimal.ZERO, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(WORK_DESCRIPTION, BigDecimal.ZERO, null, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(WORK_DESCRIPTION, negativeCost, BigDecimal.ZERO, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(WORK_DESCRIPTION, BigDecimal.ZERO, negativeCost, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(" ", BigDecimal.ZERO, BigDecimal.ZERO, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(WORK_DESCRIPTION, BigDecimal.ZERO, BigDecimal.ZERO, null));

        // Then
        invalidWorks.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Rejects priority changes on a completed order")
    void rejectsPriorityChangeOnCompletedOrder() {
        // Given
        RepairOrder order = new RepairOrder(UUID.randomUUID(), car);
        ServiceWork work = new ServiceWork(
                WORK_DESCRIPTION,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                MechanicSpecialization.GENERAL);
        Mechanic mechanic =
                new Mechanic(UUID.randomUUID(), CUSTOMER_NAME, MechanicSpecialization.GENERAL);
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        order.startProgress();
        work.complete();
        order.complete(COMPLETION_PRICE, COMPLETION_TIME);

        // When
        Runnable changePriority = () -> order.changePriority(OrderPriority.URGENT);

        // Then
        assertThrows(
                OrderModificationNotAllowedException.class,
                changePriority::run);
    }

    @Test
    @DisplayName("Completes work on an in-progress order")
    void completesWorkWhileInProgress() {
        // Given
        RepairOrder order = createOrderInStatus(OrderStatus.IN_PROGRESS);
        ServiceWork work = order.getWorks().get(0);

        // When
        order.completeWork(0);

        // Then
        assertEquals(WorkStatus.COMPLETED, work.getStatus());
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"CREATED", "DIAGNOSED", "APPROVED", "COMPLETED", "CANCELLED"})
    @DisplayName("Rejects completing work outside in-progress status")
    void rejectsCompletingWorkOutsideProgress(OrderStatus status) {
        // Given
        RepairOrder order = createOrderInStatus(status);
        Class<? extends DomainException> expectedExceptionType =
                status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED
                        ? OrderModificationNotAllowedException.class
                        : InvalidStateTransitionException.class;

        // When
        Runnable completeWork = () -> order.completeWork(0);

        // Then
        assertThrows(expectedExceptionType, completeWork::run);
    }

    @ParameterizedTest
    @CsvSource({"-1", "1"})
    @DisplayName("Rejects work indexes outside the order work list")
    void rejectsOutOfRangeWorkIndex(int workIndex) {
        // Given
        RepairOrder order = createOrderInStatus(OrderStatus.IN_PROGRESS);
        Runnable completeWork = () -> order.completeWork(workIndex);

        // When
        InvalidArgumentException exception =
                assertThrows(InvalidArgumentException.class, completeWork::run);

        // Then
        assertEquals(DomainMessages.WORK_INDEX_OUT_OF_RANGE, exception.getMessage());
    }

    @Test
    @DisplayName("Completes an order and stores its price and completion time")
    void completesOrderWithDetails() {
        // Given
        RepairOrder order = createOrderInStatus(OrderStatus.IN_PROGRESS);
        order.completeWork(0);

        // When
        order.complete(COMPLETION_PRICE, COMPLETION_TIME);

        // Then
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        assertEquals(COMPLETION_PRICE, order.getFinalPrice());
        assertEquals(COMPLETION_TIME, order.getCompletedAt());
    }

    @Test
    @DisplayName("Allows completion when an in-progress order has completed all work")
    void allowsCompletionWhenAllWorkIsComplete() {
        // Given
        RepairOrder order = createOrderInStatus(OrderStatus.IN_PROGRESS);
        order.completeWork(0);

        // When / Then
        assertDoesNotThrow(order::ensureCanComplete);
    }

    @ParameterizedTest
    @EnumSource(
            value = OrderStatus.class,
            names = {"CREATED", "DIAGNOSED", "APPROVED", "COMPLETED", "CANCELLED"})
    @DisplayName("Rejects completion outside in-progress status")
    void rejectsCompletionOutsideProgress(OrderStatus status) {
        // Given
        RepairOrder order = createOrderInStatus(status);

        // When
        InvalidStateTransitionException exception =
                assertThrows(InvalidStateTransitionException.class, order::ensureCanComplete);

        // Then
        assertEquals(DomainMessages.INVALID_COMPLETE_STATUS, exception.getMessage());
    }

    @Test
    @DisplayName("Rejects completion while any work is unfinished")
    void rejectsCompletionWithUnfinishedWork() {
        // Given
        RepairOrder order = createOrderInStatus(OrderStatus.IN_PROGRESS);

        // When
        InvalidStateTransitionException exception =
                assertThrows(InvalidStateTransitionException.class, order::ensureCanComplete);

        // Then
        assertEquals(DomainMessages.UNFINISHED_WORK, exception.getMessage());
    }

    @Test
    @DisplayName("Rejects missing completion details without changing order state")
    void rejectsMissingCompletionDetails() {
        // Given
        RepairOrder order = createOrderInStatus(OrderStatus.IN_PROGRESS);
        order.completeWork(0);

        // When
        Runnable completeWithoutPrice = () -> order.complete(null, COMPLETION_TIME);

        // Then
        assertThrows(InvalidArgumentException.class, completeWithoutPrice::run);
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
    }

    private RepairOrder createOrderInStatus(OrderStatus status) {
        RepairOrder order = new RepairOrder(UUID.randomUUID(), car);
        order.addWork(new ServiceWork(
                WORK_DESCRIPTION,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                MechanicSpecialization.GENERAL));
        if (status == OrderStatus.CANCELLED) {
            order.cancel();
            return order;
        }
        if (status != OrderStatus.CREATED) {
            order.diagnose();
        }
        if (status == OrderStatus.APPROVED
                || status == OrderStatus.IN_PROGRESS
                || status == OrderStatus.COMPLETED) {
            order.approve();
            order.assignMechanic(new Mechanic(
                    UUID.randomUUID(),
                    CUSTOMER_NAME,
                    MechanicSpecialization.GENERAL));
        }
        if (status == OrderStatus.IN_PROGRESS || status == OrderStatus.COMPLETED) {
            order.startProgress();
        }
        if (status == OrderStatus.COMPLETED) {
            order.completeWork(0);
            order.complete(COMPLETION_PRICE, COMPLETION_TIME);
        }
        return order;
    }

    private ServiceWork createWork(MechanicSpecialization specialization) {
        return new ServiceWork(
                WORK_DESCRIPTION,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                specialization);
    }

    private class ExecutableCalls {
        private java.util.List<org.junit.jupiter.api.function.Executable> calls =
                new java.util.ArrayList<>();

        private void add(org.junit.jupiter.api.function.Executable call) {
            calls.add(call);
        }

        private void assertEachThrowsInvalidArgument() {
            for (org.junit.jupiter.api.function.Executable call : calls) {
                assertThrows(InvalidArgumentException.class, call);
            }
        }
    }
}
