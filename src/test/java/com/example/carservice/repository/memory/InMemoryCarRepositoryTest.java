package com.example.carservice.repository.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryCarRepositoryTest {
    private String vin = "VIN-CAR-REPOSITORY";
    private InMemoryCarRepository repository;
    private Car car;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCarRepository();
        Customer customer = new Customer("customer-car-repository", "Car Customer", "555-0100");
        car = new Car(vin, "Test", "Model", 2020, customer);
    }

    @Test
    @DisplayName("Saves and finds car by VIN")
    void savesAndFindsCar() {
        // Given
        repository.save(car);

        // When
        Car found = repository.findByVin(vin).orElseThrow();

        // Then
        assertEquals(car, found);
    }

    @Test
    @DisplayName("Rejects a duplicate VIN")
    void rejectsDuplicateVin() {
        // Given
        repository.save(car);
        Customer customer = new Customer("other-customer", "Other Customer", "555-0101");
        Car duplicate = new Car(vin, "Other", "Model", 2021, customer);

        // When
        Runnable saveDuplicate = () -> repository.save(duplicate);

        // Then
        assertThrows(EntityAlreadyExistsException.class, saveDuplicate::run);
    }

    @Test
    @DisplayName("Returns empty for an unknown VIN")
    void unknownVinReturnsEmpty() {
        // Given
        String unknownVin = "UNKNOWN";

        // When
        boolean isEmpty = repository.findByVin(unknownVin).isEmpty();

        // Then
        assertTrue(isEmpty);
    }

    @Test
    @DisplayName("Rejects null arguments")
    void rejectsNullArguments() {
        // Given
        String nullVin = null;

        // When
        Runnable saveNull = () -> repository.save(null);
        Runnable findNull = () -> repository.findByVin(nullVin);

        // Then
        assertThrows(InvalidArgumentException.class, saveNull::run);
        assertThrows(InvalidArgumentException.class, findNull::run);
    }
}
