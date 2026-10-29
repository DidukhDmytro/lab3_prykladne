package com.example.carservice.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.exception.DomainException;
import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class DomainInvariantTest {
    private String customerId = "customer-domain";
    private String customerName = "Domain Customer";
    private String phoneNumber = "555-0100";
    private String vin = "VIN-DOMAIN";
    private String workDescription = "Domain work";
    private Customer customer;
    private Car car;

    @BeforeEach
    void setUp() {
        customer = new Customer(customerId, customerName, phoneNumber);
        car = new Car(vin, "Test", "Model", 2020, customer);
    }

    @Test
    @DisplayName("Validates customer constructor arguments")
    void validatesCustomerConstructor() {
        // Given
        String blankId = " ";
        String blankName = " ";
        String blankPhone = " ";

        // When
        ExecutableCalls invalidCustomers = new ExecutableCalls();
        invalidCustomers.add(() -> new Customer(null, customerName, phoneNumber));
        invalidCustomers.add(() -> new Customer(blankId, customerName, phoneNumber));
        invalidCustomers.add(() -> new Customer(customerId, null, phoneNumber));
        invalidCustomers.add(() -> new Customer(customerId, blankName, phoneNumber));
        invalidCustomers.add(() -> new Customer(customerId, customerName, null));
        invalidCustomers.add(() -> new Customer(customerId, customerName, blankPhone));

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
        invalidCars.add(() -> new Car(vin, "Test", "Model", 2020, null));

        // Then
        invalidCars.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Validates mechanic constructor arguments")
    void validatesMechanicConstructor() {
        // Given
        String blankId = " ";
        String blankName = " ";

        // When
        ExecutableCalls invalidMechanics = new ExecutableCalls();
        invalidMechanics.add(() ->
                new Mechanic(null, customerName, MechanicSpecialization.GENERAL));
        invalidMechanics.add(() ->
                new Mechanic(blankId, customerName, MechanicSpecialization.GENERAL));
        invalidMechanics.add(() ->
                new Mechanic("mechanic-domain", null, MechanicSpecialization.GENERAL));
        invalidMechanics.add(() ->
                new Mechanic("mechanic-domain", blankName, MechanicSpecialization.GENERAL));
        invalidMechanics.add(() ->
                new Mechanic("mechanic-domain", customerName, null));

        // Then
        invalidMechanics.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Validates repair order constructor arguments")
    void validatesRepairOrderConstructor() {
        // Given
        String blankId = " ";

        // When
        ExecutableCalls invalidOrders = new ExecutableCalls();
        invalidOrders.add(() -> new RepairOrder(null, car));
        invalidOrders.add(() -> new RepairOrder(blankId, car));
        invalidOrders.add(() -> new RepairOrder("order-domain", null));

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
                new ServiceWork(workDescription, zeroCost, zeroCost, MechanicSpecialization.GENERAL);

        // Then
        assertDoesNotThrow(constructZeroCostWork::run);
    }

    @Test
    @DisplayName("Rejects adding null service work")
    void rejectsNullServiceWork() {
        // Given
        RepairOrder order = new RepairOrder("order-null-work", car);

        // When
        Runnable addNullWork = () -> order.addWork(null);

        // Then
        assertThrows(InvalidArgumentException.class, addNullWork::run);
    }

    @Test
    @DisplayName("Rejects assigning a null mechanic")
    void rejectsNullMechanicAssignment() {
        // Given
        RepairOrder order = new RepairOrder("order-null-mechanic", car);

        // When
        Runnable assignNullMechanic = () -> order.assignMechanic(null);

        // Then
        assertThrows(InvalidArgumentException.class, assignNullMechanic::run);
    }

    @Test
    @DisplayName("Does not add incompatible work after mechanic assignment")
    void rejectsIncompatibleWorkAfterMechanicAssignment() {
        // Given
        RepairOrder order = new RepairOrder("order-incompatible-work", car);
        Mechanic mechanic =
                new Mechanic("mechanic-engine", customerName, MechanicSpecialization.ENGINE);
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
        RepairOrder order = new RepairOrder("order-compatible-work", car);
        Mechanic mechanic =
                new Mechanic("mechanic-compatible", customerName, mechanicSpecialization);
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
                new ServiceWork(workDescription, null, BigDecimal.ZERO, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(workDescription, BigDecimal.ZERO, null, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(workDescription, negativeCost, BigDecimal.ZERO, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(workDescription, BigDecimal.ZERO, negativeCost, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(" ", BigDecimal.ZERO, BigDecimal.ZERO, MechanicSpecialization.GENERAL));
        invalidWorks.add(() ->
                new ServiceWork(workDescription, BigDecimal.ZERO, BigDecimal.ZERO, null));

        // Then
        invalidWorks.assertEachThrowsInvalidArgument();
    }

    @Test
    @DisplayName("Rejects priority changes on a completed order")
    void rejectsPriorityChangeOnCompletedOrder() {
        // Given
        RepairOrder order = new RepairOrder("order-completed", car);
        ServiceWork work = new ServiceWork(
                workDescription,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                MechanicSpecialization.GENERAL);
        Mechanic mechanic =
                new Mechanic("mechanic-completed", customerName, MechanicSpecialization.GENERAL);
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        order.startProgress();
        work.complete();
        order.complete();

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
        assertEquals(DomainMessages.workIndexOutOfRange(), exception.getMessage());
    }

    private RepairOrder createOrderInStatus(OrderStatus status) {
        RepairOrder order = new RepairOrder("order-complete-work", car);
        order.addWork(new ServiceWork(
                workDescription,
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
                    "mechanic-complete-work",
                    customerName,
                    MechanicSpecialization.GENERAL));
        }
        if (status == OrderStatus.IN_PROGRESS || status == OrderStatus.COMPLETED) {
            order.startProgress();
        }
        if (status == OrderStatus.COMPLETED) {
            order.completeWork(0);
            order.complete();
        }
        return order;
    }

    private ServiceWork createWork(MechanicSpecialization specialization) {
        return new ServiceWork(
                workDescription,
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
