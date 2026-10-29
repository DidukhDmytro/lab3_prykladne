package com.example.carservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Gift;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.WorkStatus;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.MechanicRepository;
import com.example.carservice.repository.RepairOrderRepository;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepairOrderServiceTest {
    private String orderId = "order-test";
    private String vin = "VIN-TEST";
    private String mechanicId = "mechanic-test";
    private String workDescription = "Test repair";
    private int firstWorkIndex = 0;
    private BigDecimal workParts = new BigDecimal("10.00");
    private BigDecimal workLabor = new BigDecimal("5.00");

    @Mock
    private RepairOrderRepository repairOrderRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @Mock
    private PricingPolicy pricingPolicy;

    @Mock
    private GiftPolicy giftPolicy;

    private RepairOrder order;
    private RepairOrderService service;
    private ServiceWork work;
    private Mechanic mechanic;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("customer-test", "Test Customer", "555-0100");
        Car car = new Car(vin, "Test", "Model", 2020, customer);
        order = new RepairOrder(orderId, car);
        work = createWork();
        mechanic = new Mechanic(mechanicId, "Test Mechanic", MechanicSpecialization.GENERAL);
        service = new RepairOrderService(
                repairOrderRepository,
                carRepository,
                mechanicRepository,
                pricingPolicy,
                giftPolicy);
    }

    @ParameterizedTest(name = "allows {0} from {1} to {2}")
    @CsvSource({
            "DIAGNOSE,CREATED,DIAGNOSED",
            "APPROVE,DIAGNOSED,APPROVED",
            "START_PROGRESS,APPROVED,IN_PROGRESS",
            "COMPLETE,IN_PROGRESS,COMPLETED",
            "CANCEL,CREATED,CANCELLED",
            "CANCEL,DIAGNOSED,CANCELLED",
            "CANCEL,APPROVED,CANCELLED"
    })
    @DisplayName("Allows valid repair order status transitions")
    void allowsValidTransition(
            OrderOperation operation,
            OrderStatus initial,
            OrderStatus expected) {
        // Given
        prepareFor(initial, operation);

        // When
        perform(operation);

        // Then
        assertEquals(expected, order.getStatus());
        verify(repairOrderRepository).save(order);
    }

    @ParameterizedTest(name = "rejects {0} while status is {1}")
    @CsvSource({
            "APPROVE,CREATED",
            "START_PROGRESS,CREATED",
            "COMPLETE,CREATED",
            "DIAGNOSE,DIAGNOSED",
            "START_PROGRESS,DIAGNOSED",
            "COMPLETE,DIAGNOSED",
            "DIAGNOSE,APPROVED",
            "APPROVE,APPROVED",
            "COMPLETE,APPROVED",
            "DIAGNOSE,IN_PROGRESS",
            "APPROVE,IN_PROGRESS",
            "START_PROGRESS,IN_PROGRESS",
            "CANCEL,IN_PROGRESS",
            "DIAGNOSE,COMPLETED",
            "APPROVE,COMPLETED",
            "START_PROGRESS,COMPLETED",
            "COMPLETE,COMPLETED",
            "CANCEL,COMPLETED",
            "DIAGNOSE,CANCELLED",
            "APPROVE,CANCELLED",
            "START_PROGRESS,CANCELLED",
            "COMPLETE,CANCELLED",
            "CANCEL,CANCELLED",
            "CHANGE_PRIORITY,COMPLETED",
            "CHANGE_PRIORITY,CANCELLED"
    })
    @DisplayName("Rejects forbidden repair order status transitions")
    void rejectsForbiddenTransition(OrderOperation operation, OrderStatus initial) {
        // Given
        prepareFor(initial, operation);

        // When
        Runnable forbiddenOperation = () -> perform(operation);

        // Then
        if (operation == OrderOperation.CHANGE_PRIORITY) {
            assertThrows(OrderModificationNotAllowedException.class, forbiddenOperation::run);
        } else {
            assertThrows(InvalidStateTransitionException.class, forbiddenOperation::run);
        }
    }

    @Test
    @DisplayName("Does not complete work while any work item is unfinished")
    void rejectsCompletionWithUnfinishedWork() {
        // Given
        stubExistingOrder();
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        order.startProgress();

        // When
        Runnable completeOrder = () -> service.complete(orderId);

        // Then
        assertThrows(InvalidStateTransitionException.class, completeOrder::run);
    }

    @Test
    @DisplayName("Rejects creating an order with an existing id")
    void rejectsDuplicateOrderId() {
        // Given
        stubExistingOrder();
        Runnable createDuplicate = () -> service.createOrder(orderId, vin);

        // When
        EntityAlreadyExistsException exception =
                assertThrows(EntityAlreadyExistsException.class, createDuplicate::run);

        // Then
        assertEquals(
                String.format(ServiceMessages.duplicateRepairOrder(), orderId),
                exception.getMessage());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fails to create an order for an unknown VIN")
    void createOrderFailsForUnknownVin() {
        // Given
        when(repairOrderRepository.findById(orderId)).thenReturn(Optional.empty());
        when(carRepository.findByVin(vin)).thenReturn(Optional.empty());
        Runnable createOrder = () -> service.createOrder(orderId, vin);

        // When
        EntityNotFoundException exception =
                assertThrows(EntityNotFoundException.class, createOrder::run);

        // Then
        assertEquals(String.format(ServiceMessages.carNotFound(), vin), exception.getMessage());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Saves a new order when its id is free")
    void createsOrderWhenIdIsFree() {
        // Given
        when(repairOrderRepository.findById(orderId)).thenReturn(Optional.empty());
        when(carRepository.findByVin(vin)).thenReturn(Optional.of(order.getCar()));
        ArgumentCaptor<RepairOrder> savedOrder = ArgumentCaptor.forClass(RepairOrder.class);

        // When
        service.createOrder(orderId, vin);

        // Then
        verify(repairOrderRepository).save(savedOrder.capture());
        assertEquals(orderId, savedOrder.getValue().getId());
        assertSame(order.getCar(), savedOrder.getValue().getCar());
        assertNotSame(order, savedOrder.getValue());
    }

    @Test
    @DisplayName("Completes work through the order and saves it")
    void completesWorkAndSavesOrder() {
        // Given
        stubExistingOrder();
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        order.startProgress();
        when(repairOrderRepository.save(order)).thenReturn(order);

        // When
        RepairOrder result = service.completeWork(orderId, firstWorkIndex);

        // Then
        assertSame(order, result);
        assertEquals(WorkStatus.COMPLETED, work.getStatus());
        verify(repairOrderRepository).save(order);
    }

    @Test
    @DisplayName("Requires a mechanic before starting progress")
    void requiresMechanicBeforeProgress() {
        // Given
        stubExistingOrder();
        order.addWork(work);
        order.diagnose();
        order.approve();

        // When
        Runnable startOrder = () -> service.startProgress(orderId);

        // Then
        assertThrows(InvalidStateTransitionException.class, startOrder::run);
    }

    @Test
    @DisplayName("Does not save when order is missing")
    void missingOrderDoesNotSave() {
        // Given
        when(repairOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When
        Runnable diagnoseMissing = () -> service.diagnose(orderId);
        assertThrows(EntityNotFoundException.class, diagnoseMissing::run);

        // Then
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Saves order after a successful operation")
    void successfulOperationSavesOrder() {
        // Given
        stubExistingOrder();
        when(repairOrderRepository.save(order)).thenReturn(order);

        // When
        RepairOrder result = service.addWork(orderId, work);

        // Then
        assertSame(order, result);
        verify(repairOrderRepository).save(order);
    }

    @Test
    @DisplayName("Rejects assigning an unknown mechanic")
    void rejectsUnknownMechanicAssignment() {
        // Given
        stubExistingOrder();
        when(mechanicRepository.findById(mechanicId)).thenReturn(Optional.empty());

        // When
        Runnable assignUnknownMechanic = () -> service.assignMechanic(orderId, mechanicId);

        // Then
        assertThrows(EntityNotFoundException.class, assignUnknownMechanic::run);
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assigns a mechanic and saves the order")
    void assignsMechanicAndSavesOrder() {
        // Given
        stubExistingOrder();
        when(mechanicRepository.findById(mechanicId)).thenReturn(Optional.of(mechanic));
        when(repairOrderRepository.save(order)).thenReturn(order);

        // When
        RepairOrder result = service.assignMechanic(orderId, mechanicId);

        // Then
        assertSame(order, result);
        assertSame(mechanic, order.getAssignedMechanic());
        verify(repairOrderRepository).save(order);
    }

    @Test
    @DisplayName("Changes priority and saves the order")
    void changesPriorityAndSavesOrder() {
        // Given
        stubExistingOrder();
        when(repairOrderRepository.save(order)).thenReturn(order);

        // When
        RepairOrder result = service.changePriority(orderId, OrderPriority.URGENT);

        // Then
        assertSame(order, result);
        assertEquals(OrderPriority.URGENT, order.getPriority());
        verify(repairOrderRepository).save(order);
    }

    @Test
    @DisplayName("Delegates status lookup to the repository")
    void delegatesFindByStatus() {
        // Given
        List<RepairOrder> expectedOrders = List.of(order);
        when(repairOrderRepository.findByStatus(OrderStatus.DIAGNOSED))
                .thenReturn(expectedOrders);

        // When
        List<RepairOrder> result = service.findByStatus(OrderStatus.DIAGNOSED);

        // Then
        assertSame(expectedOrders, result);
        verify(repairOrderRepository).findByStatus(OrderStatus.DIAGNOSED);
    }

    @Test
    @DisplayName("Creates a present through the gift policy")
    void createsPresentThroughGiftPolicy() {
        // Given
        stubExistingOrder();
        Present expectedPresent = new Present(workParts, new Gift("Test gift", workParts));
        when(giftPolicy.createPresent(order)).thenReturn(expectedPresent);

        // When
        Present result = service.createPresent(orderId);

        // Then
        assertSame(expectedPresent, result);
        verify(giftPolicy).createPresent(order);
    }

    @Test
    @DisplayName("Delegates base price calculation to calculator")
    void delegatesBasePriceCalculation() {
        // Given
        stubExistingOrder();
        BigDecimal expectedPrice = new BigDecimal("15.00");
        when(pricingPolicy.calculateBasePrice(order)).thenReturn(expectedPrice);

        // When
        BigDecimal actualPrice = service.calculateBasePrice(orderId);

        // Then
        assertEquals(expectedPrice, actualPrice);
        verify(pricingPolicy).calculateBasePrice(order);
    }

    @Test
    @DisplayName("Delegates calculated price to calculator")
    void delegatesFinalPriceCalculation() {
        // Given
        stubExistingOrder();
        BigDecimal expectedPrice = new BigDecimal("18.00");
        when(pricingPolicy.calculateFinalPrice(order)).thenReturn(expectedPrice);

        // When
        BigDecimal actualPrice = service.calculateFinalPrice(orderId);

        // Then
        assertEquals(expectedPrice, actualPrice);
        verify(pricingPolicy).calculateFinalPrice(order);
    }

    private void prepareFor(OrderStatus status, OrderOperation operation) {
        switch (status) {
            case CREATED -> {
            }
            case DIAGNOSED -> {
                order.addWork(createWork());
                order.diagnose();
            }
            case APPROVED -> prepareApprovedOrder();
            case IN_PROGRESS -> {
                prepareApprovedOrder();
                order.startProgress();
                if (operation == OrderOperation.COMPLETE) {
                    order.completeWork(firstWorkIndex);
                }
            }
            case COMPLETED -> {
                prepareApprovedOrder();
                order.startProgress();
                order.completeWork(firstWorkIndex);
                order.complete();
            }
            case CANCELLED -> order.cancel();
        }
    }

    private void prepareApprovedOrder() {
        order.addWork(createWork());
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
    }

    private void perform(OrderOperation operation) {
        stubExistingOrder();
        operationMap().get(operation).accept(service, orderId);
    }

    private void stubExistingOrder() {
        when(repairOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
    }

    private Map<OrderOperation, BiConsumer<RepairOrderService, String>> operationMap() {
        Map<OrderOperation, BiConsumer<RepairOrderService, String>> operations =
                new EnumMap<>(OrderOperation.class);
        operations.put(OrderOperation.DIAGNOSE, RepairOrderService::diagnose);
        operations.put(OrderOperation.APPROVE, RepairOrderService::approve);
        operations.put(OrderOperation.START_PROGRESS, RepairOrderService::startProgress);
        operations.put(OrderOperation.COMPLETE, RepairOrderService::complete);
        operations.put(OrderOperation.CANCEL, RepairOrderService::cancel);
        operations.put(
                OrderOperation.CHANGE_PRIORITY,
                (repairOrderService, id) ->
                        repairOrderService.changePriority(id, OrderPriority.URGENT));
        return operations;
    }

    private ServiceWork createWork() {
        return new ServiceWork(workDescription, workParts, workLabor, MechanicSpecialization.GENERAL);
    }

    private enum OrderOperation {
        DIAGNOSE,
        APPROVE,
        START_PROGRESS,
        COMPLETE,
        CANCEL,
        CHANGE_PRIORITY
    }
}
