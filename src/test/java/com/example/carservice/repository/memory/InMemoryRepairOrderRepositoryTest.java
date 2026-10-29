package com.example.carservice.repository.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.exception.InvalidArgumentException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryRepairOrderRepositoryTest {
    private String orderId = "order-repository";
    private InMemoryRepairOrderRepository repository;
    private RepairOrder order;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepairOrderRepository();
        Customer customer = new Customer("customer-repository", "Repository Customer", "555-0100");
        Car car = new Car("VIN-REPOSITORY", "Test", "Model", 2020, customer);
        order = new RepairOrder(orderId, car);
    }

    @Test
    @DisplayName("Saves and finds an order by id")
    void savesAndFindsById() {
        // Given
        repository.save(order);

        // When
        RepairOrder result = repository.findById(orderId).orElseThrow();

        // Then
        assertEquals(order, result);
    }

    @Test
    @DisplayName("Saving the same id overwrites without increasing repository size")
    void overwritesExistingId() {
        // Given
        repository.save(order);
        RepairOrder replacement = new RepairOrder(orderId, order.getCar());
        replacement.diagnose();

        // When
        repository.save(replacement);

        // Then
        assertEquals(1, repository.findAll().size());
        assertEquals(OrderStatus.DIAGNOSED, repository.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Returns empty for an unknown id")
    void unknownIdReturnsEmpty() {
        // Given
        String unknownId = "unknown";

        // When
        boolean isEmpty = repository.findById(unknownId).isEmpty();

        // Then
        assertTrue(isEmpty);
    }

    @Test
    @DisplayName("Finds orders by status")
    void findsByStatus() {
        // Given
        repository.save(order);
        RepairOrder diagnosedOrder = new RepairOrder("diagnosed-order", order.getCar());
        diagnosedOrder.diagnose();
        repository.save(diagnosedOrder);

        // When
        List<RepairOrder> diagnosed = repository.findByStatus(OrderStatus.DIAGNOSED);

        // Then
        assertEquals(List.of(diagnosedOrder), diagnosed);
    }

    @Test
    @DisplayName("Returns an unmodifiable copy from findAll")
    void findAllIsUnmodifiableCopy() {
        // Given
        repository.save(order);
        List<RepairOrder> results = repository.findAll();

        // When
        Runnable addToResults = () -> results.add(order);
        repository.save(new RepairOrder("later-order", order.getCar()));

        // Then
        assertThrows(UnsupportedOperationException.class, addToResults::run);
        assertEquals(1, results.size());
        assertFalse(results == repository.findAll());
    }

    @Test
    @DisplayName("Rejects null repository arguments")
    void rejectsNullArguments() {
        // Given
        String id = null;

        // When
        Runnable saveNull = () -> repository.save(null);
        Runnable findNull = () -> repository.findById(id);
        Runnable findNullStatus = () -> repository.findByStatus(null);

        // Then
        assertThrows(InvalidArgumentException.class, saveNull::run);
        assertThrows(InvalidArgumentException.class, findNull::run);
        assertThrows(InvalidArgumentException.class, findNullStatus::run);
    }
}
