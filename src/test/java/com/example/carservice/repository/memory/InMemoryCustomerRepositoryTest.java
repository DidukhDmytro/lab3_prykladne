package com.example.carservice.repository.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryCustomerRepositoryTest {
    private String customerId = "customer-repository";
    private InMemoryCustomerRepository repository;
    private Customer customer;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCustomerRepository();
        customer = new Customer(customerId, "Repository Customer", "555-0100");
    }

    @Test
    @DisplayName("Saves and finds customer by id")
    void savesAndFindsCustomer() {
        // Given
        repository.save(customer);

        // When
        Customer found = repository.findById(customerId).orElseThrow();

        // Then
        assertSame(customer, found);
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("Returns empty for an unknown customer id")
    void unknownIdReturnsEmpty() {
        // Given
        String unknownId = "unknown-customer";

        // When
        boolean isEmpty = repository.findById(unknownId).isEmpty();

        // Then
        assertTrue(isEmpty);
    }

    @Test
    @DisplayName("Saving an existing id replaces the customer")
    void replacesExistingCustomer() {
        // Given
        repository.save(customer);
        Customer replacement = new Customer(customerId, "Replacement Customer", "555-0101");

        // When
        repository.save(replacement);

        // Then
        assertSame(replacement, repository.findById(customerId).orElseThrow());
        assertEquals(1, repository.findAll().size());
    }
}
