package com.example.carservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Gift;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.domain.enumerate.OrderPriority;
import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.domain.enumerate.WorkStatus;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.MechanicRepository;
import com.example.carservice.repository.RepairOrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepairOrderServiceTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-29T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final String VIN = "VIN-TEST";
    private static final UUID MECHANIC_ID = UUID.randomUUID();
    private static final String WORK_DESCRIPTION = "Test repair";
    private static final int FIRST_WORK_INDEX = 0;
    private static final BigDecimal WORK_PARTS = new BigDecimal("10.00");
    private static final BigDecimal WORK_LABOR = new BigDecimal("5.00");

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
        Customer customer = new Customer(UUID.randomUUID(), "Test Customer", "555-0100");
        Car car = new Car(VIN, "Test", "Model", 2020, customer);
        order = new RepairOrder(ORDER_ID, car);
        work = createWork();
        mechanic = new Mechanic(MECHANIC_ID, "Test Mechanic", MechanicSpecialization.GENERAL);
        service = new RepairOrderService(
                repairOrderRepository,
                carRepository,
                mechanicRepository,
                pricingPolicy,
                giftPolicy,
                FIXED_CLOCK);
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
            String operation,
            OrderStatus initial,
            OrderStatus expected) {
        // Given
        prepareFor(initial, operation);
        if ("COMPLETE".equals(operation)) {
            when(pricingPolicy.calculateFinalPrice(order)).thenReturn(BigDecimal.ZERO);
        }

        // When
        perform(operation);

        // Then
        assertEquals(expected, order.getStatus());
        if ("COMPLETE".equals(operation)) {
            assertEquals(BigDecimal.ZERO, order.getFinalPrice());
            assertEquals(FIXED_INSTANT, order.getCompletedAt());
        }
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
    void rejectsForbiddenTransition(String operation, OrderStatus initial) {
        // Given
        prepareFor(initial, operation);

        // When
        Runnable forbiddenOperation = () -> perform(operation);

        // Then
        if ("CHANGE_PRIORITY".equals(operation)) {
            assertThrows(OrderModificationNotAllowedException.class, forbiddenOperation::run);
        } else {
            assertThrows(InvalidStateTransitionException.class, forbiddenOperation::run);
        }
        if ("COMPLETE".equals(operation)) {
            verify(pricingPolicy, never()).calculateFinalPrice(any());
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
        Runnable completeOrder = () -> service.complete(ORDER_ID);

        // Then
        assertThrows(InvalidStateTransitionException.class, completeOrder::run);
        verify(pricingPolicy, never()).calculateFinalPrice(any());
    }

    @Test
    @DisplayName("Does not save completion when final pricing fails")
    void pricingFailurePreventsCompletionSave() {
        // Given
        stubExistingOrder();
        order.addWork(work);
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
        order.startProgress();
        order.completeWork(FIRST_WORK_INDEX);
        when(pricingPolicy.calculateFinalPrice(order)).thenThrow(new IllegalStateException());

        // When
        Runnable completeOrder = () -> service.complete(ORDER_ID);

        // Then
        assertThrows(IllegalStateException.class, completeOrder::run);
        assertEquals(OrderStatus.IN_PROGRESS, order.getStatus());
        assertTrue(mechanic.isBusy());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adds and saves a batch of work once")
    void addsWorksAndSavesOnce() {
        // Given
        stubExistingOrder();
        ServiceWork secondWork = new ServiceWork(
                "Second repair",
                BigDecimal.ONE,
                BigDecimal.ONE,
                MechanicSpecialization.GENERAL);
        List<ServiceWork> works = List.of(work, secondWork);
        when(repairOrderRepository.save(order)).thenReturn(order);

        // When
        RepairOrder result = service.addWorks(ORDER_ID, works);

        // Then
        assertSame(order, result);
        assertEquals(works, order.getWorks());
        verify(repairOrderRepository).save(order);
    }

    @Test
    @DisplayName("Does not save a batch when an element is invalid")
    void invalidWorkPreventsBatchSave() {
        // Given
        stubExistingOrder();
        order.assignMechanic(new Mechanic(
                MECHANIC_ID,
                "Engine Mechanic",
                MechanicSpecialization.ENGINE));
        ServiceWork incompatibleWork = new ServiceWork(
                "Brake repair",
                BigDecimal.ONE,
                BigDecimal.ONE,
                MechanicSpecialization.BRAKES);
        List<ServiceWork> works = List.of(work, incompatibleWork);

        // When
        Runnable addInvalidBatch = () -> service.addWorks(ORDER_ID, works);

        // Then
        assertThrows(MechanicNotCompatibleException.class, addInvalidBatch::run);
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejects null and empty work batches")
    void rejectsMissingWorkBatch() {
        // Given
        List<ServiceWork> nullWorks = null;
        List<ServiceWork> emptyWorks = List.of();

        // When
        Runnable addNullBatch = () -> service.addWorks(ORDER_ID, nullWorks);
        Runnable addEmptyBatch = () -> service.addWorks(ORDER_ID, emptyWorks);

        // Then
        assertThrows(InvalidArgumentException.class, addNullBatch::run);
        assertThrows(InvalidArgumentException.class, addEmptyBatch::run);
        verifyNoInteractions(repairOrderRepository, carRepository, mechanicRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE_ORDER",
            "FIND_ORDER",
            "ADD_WORK",
            "ADD_WORKS",
            "CHANGE_PRIORITY",
            "DIAGNOSE",
            "APPROVE",
            "ASSIGN_MECHANIC",
            "START_PROGRESS",
            "COMPLETE_WORK",
            "COMPLETE",
            "CANCEL",
            "CALCULATE_BASE_PRICE",
            "CALCULATE_FINAL_PRICE",
            "CREATE_PRESENT"
    })
    @DisplayName("Rejects null order identifiers before repository access")
    void rejectsNullOrderIdentifiers(String operation) {
        // When
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> performWithNullOrderIdentifier(operation));

        // Then
        assertEquals(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED, exception.getMessage());
        verifyNoInteractions(repairOrderRepository, carRepository, mechanicRepository);
    }

    @Test
    @DisplayName("Rejects a null VIN before repository access")
    void rejectsNullVinBeforeRepositoryAccess() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> service.createOrder(ORDER_ID, null));

        assertEquals(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED, exception.getMessage());
        verifyNoInteractions(repairOrderRepository, carRepository, mechanicRepository);
    }

    @Test
    @DisplayName("Rejects a null mechanic identifier before repository access")
    void rejectsNullMechanicIdentifierBeforeRepositoryAccess() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> service.assignMechanic(ORDER_ID, null));

        assertEquals(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED, exception.getMessage());
        verifyNoInteractions(repairOrderRepository, carRepository, mechanicRepository);
    }

    @Test
    @DisplayName("Rejects a null priority before repository access")
    void rejectsNullPriorityBeforeRepositoryAccess() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> service.changePriority(ORDER_ID, null));

        assertEquals(ServiceMessages.SERVICE_PRIORITY_REQUIRED, exception.getMessage());
        verifyNoInteractions(repairOrderRepository, carRepository, mechanicRepository);
    }

    @Test
    @DisplayName("Rejects a null status before repository access")
    void rejectsNullStatusBeforeRepositoryAccess() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> service.findByStatus(null));

        assertEquals(ServiceMessages.SERVICE_STATUS_REQUIRED, exception.getMessage());
        verifyNoInteractions(repairOrderRepository, carRepository, mechanicRepository);
    }

    @Test
    @DisplayName("Rejects null work before repository access")
    void rejectsNullWorkBeforeRepositoryAccess() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> service.addWork(ORDER_ID, null));

        assertEquals(ServiceMessages.ENTITY_REQUIRED, exception.getMessage());
        verifyNoInteractions(repairOrderRepository, carRepository, mechanicRepository);
    }

    @Test
    @DisplayName("Rejects creating an order with an existing id")
    void rejectsDuplicateOrderId() {
        // Given
        stubExistingOrder();
        Runnable createDuplicate = () -> service.createOrder(ORDER_ID, VIN);

        // When
        EntityAlreadyExistsException exception =
                assertThrows(EntityAlreadyExistsException.class, createDuplicate::run);

        // Then
        assertEquals(
                String.format(ServiceMessages.DUPLICATE_REPAIR_ORDER, ORDER_ID),
                exception.getMessage());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Finds an order by identifier")
    void findsOrder() {
        stubExistingOrder();

        assertEquals(Optional.of(order), service.findOrder(ORDER_ID));
        verify(repairOrderRepository).findById(ORDER_ID);
    }

    @Test
    @DisplayName("Returns empty when an order is missing")
    void returnsEmptyForMissingOrder() {
        when(repairOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.findOrder(ORDER_ID));
    }

    @Test
    @DisplayName("Fails to create an order for an unknown VIN")
    void createOrderFailsForUnknownVin() {
        // Given
        when(repairOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());
        when(carRepository.findByVin(VIN)).thenReturn(Optional.empty());
        Runnable createOrder = () -> service.createOrder(ORDER_ID, VIN);

        // When
        EntityNotFoundException exception =
                assertThrows(EntityNotFoundException.class, createOrder::run);

        // Then
        assertEquals(String.format(ServiceMessages.CAR_NOT_FOUND, VIN), exception.getMessage());
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Saves a new order when its id is free")
    void createsOrderWhenIdIsFree() {
        // Given
        when(repairOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());
        when(carRepository.findByVin(VIN)).thenReturn(Optional.of(order.getCar()));
        ArgumentCaptor<RepairOrder> savedOrder = ArgumentCaptor.forClass(RepairOrder.class);

        // When
        service.createOrder(ORDER_ID, VIN);

        // Then
        verify(repairOrderRepository).save(savedOrder.capture());
        assertEquals(ORDER_ID, savedOrder.getValue().getId());
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
        RepairOrder result = service.completeWork(ORDER_ID, FIRST_WORK_INDEX);

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
        Runnable startOrder = () -> service.startProgress(ORDER_ID);

        // Then
        assertThrows(InvalidStateTransitionException.class, startOrder::run);
    }

    @Test
    @DisplayName("Does not save when order is missing")
    void missingOrderDoesNotSave() {
        // Given
        when(repairOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // When
        Runnable diagnoseMissing = () -> service.diagnose(ORDER_ID);
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
        RepairOrder result = service.addWork(ORDER_ID, work);

        // Then
        assertSame(order, result);
        verify(repairOrderRepository).save(order);
    }

    @Test
    @DisplayName("Rejects assigning an unknown mechanic")
    void rejectsUnknownMechanicAssignment() {
        // Given
        stubExistingOrder();
        when(mechanicRepository.findById(MECHANIC_ID)).thenReturn(Optional.empty());

        // When
        Runnable assignUnknownMechanic = () -> service.assignMechanic(ORDER_ID, MECHANIC_ID);

        // Then
        assertThrows(EntityNotFoundException.class, assignUnknownMechanic::run);
        verify(repairOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Assigns a mechanic and saves the order")
    void assignsMechanicAndSavesOrder() {
        // Given
        stubExistingOrder();
        when(mechanicRepository.findById(MECHANIC_ID)).thenReturn(Optional.of(mechanic));
        when(repairOrderRepository.save(order)).thenReturn(order);

        // When
        RepairOrder result = service.assignMechanic(ORDER_ID, MECHANIC_ID);

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
        RepairOrder result = service.changePriority(ORDER_ID, OrderPriority.URGENT);

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
        Present expectedPresent = new Present(WORK_PARTS, new Gift("Test gift", WORK_PARTS));
        when(giftPolicy.createPresent(order)).thenReturn(expectedPresent);

        // When
        Present result = service.createPresent(ORDER_ID);

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
        BigDecimal actualPrice = service.calculateBasePrice(ORDER_ID);

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
        BigDecimal actualPrice = service.calculateFinalPrice(ORDER_ID);

        // Then
        assertEquals(expectedPrice, actualPrice);
        verify(pricingPolicy).calculateFinalPrice(order);
    }

    private void prepareFor(OrderStatus status, String operation) {
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
                if ("COMPLETE".equals(operation)) {
                    order.completeWork(FIRST_WORK_INDEX);
                }
            }
            case COMPLETED -> {
                prepareApprovedOrder();
                order.startProgress();
                order.completeWork(FIRST_WORK_INDEX);
                order.complete(BigDecimal.ZERO, FIXED_INSTANT);
            }
            case CANCELLED -> order.cancel();
        }
    }

    private void performWithNullOrderIdentifier(String operation) {
        switch (operation) {
            case "CREATE_ORDER" -> service.createOrder(null, VIN);
            case "FIND_ORDER" -> service.findOrder(null);
            case "ADD_WORK" -> service.addWork(null, work);
            case "ADD_WORKS" -> service.addWorks(null, List.of(work));
            case "CHANGE_PRIORITY" -> service.changePriority(null, OrderPriority.URGENT);
            case "DIAGNOSE" -> service.diagnose(null);
            case "APPROVE" -> service.approve(null);
            case "ASSIGN_MECHANIC" -> service.assignMechanic(null, MECHANIC_ID);
            case "START_PROGRESS" -> service.startProgress(null);
            case "COMPLETE_WORK" -> service.completeWork(null, FIRST_WORK_INDEX);
            case "COMPLETE" -> service.complete(null);
            case "CANCEL" -> service.cancel(null);
            case "CALCULATE_BASE_PRICE" -> service.calculateBasePrice(null);
            case "CALCULATE_FINAL_PRICE" -> service.calculateFinalPrice(null);
            case "CREATE_PRESENT" -> service.createPresent(null);
        }
    }

    private void prepareApprovedOrder() {
        order.addWork(createWork());
        order.diagnose();
        order.approve();
        order.assignMechanic(mechanic);
    }

    private void perform(String operation) {
        stubExistingOrder();
        operationMap().get(operation).accept(service, ORDER_ID);
    }

    private void stubExistingOrder() {
        when(repairOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    private Map<String, BiConsumer<RepairOrderService, UUID>> operationMap() {
        return Map.of(
                "DIAGNOSE", RepairOrderService::diagnose,
                "APPROVE", RepairOrderService::approve,
                "START_PROGRESS", RepairOrderService::startProgress,
                "COMPLETE", RepairOrderService::complete,
                "CANCEL", RepairOrderService::cancel,
                "CHANGE_PRIORITY",
                (repairOrderService, id) ->
                        repairOrderService.changePriority(id, OrderPriority.URGENT));
    }

    private ServiceWork createWork() {
        return new ServiceWork(WORK_DESCRIPTION, WORK_PARTS, WORK_LABOR, MechanicSpecialization.GENERAL);
    }

}
