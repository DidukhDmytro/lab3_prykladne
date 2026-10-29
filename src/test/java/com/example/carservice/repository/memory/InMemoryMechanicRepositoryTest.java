package com.example.carservice.repository.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryMechanicRepositoryTest {
    private String mechanicId = "mechanic-repository";
    private InMemoryMechanicRepository repository;
    private Mechanic mechanic;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMechanicRepository();
        mechanic = new Mechanic(mechanicId, "Repository Mechanic", MechanicSpecialization.GENERAL);
    }

    @Test
    @DisplayName("Saves and finds mechanic by id")
    void savesAndFindsMechanic() {
        // Given
        repository.save(mechanic);

        // When
        Mechanic found = repository.findById(mechanicId).orElseThrow();

        // Then
        assertEquals(mechanic, found);
    }

    @Test
    @DisplayName("Saving an existing id replaces the mechanic")
    void replacesExistingMechanic() {
        // Given
        repository.save(mechanic);
        Mechanic replacement =
                new Mechanic(mechanicId, "Replacement Mechanic", MechanicSpecialization.ENGINE);

        // When
        repository.save(replacement);

        // Then
        assertEquals(replacement, repository.findById(mechanicId).orElseThrow());
    }

    @Test
    @DisplayName("Returns empty for an unknown mechanic id")
    void unknownIdReturnsEmpty() {
        // Given
        String unknownId = "unknown";

        // When
        boolean isEmpty = repository.findById(unknownId).isEmpty();

        // Then
        assertTrue(isEmpty);
    }

    @Test
    @DisplayName("Rejects null arguments")
    void rejectsNullArguments() {
        // Given
        String nullId = null;

        // When
        Runnable saveNull = () -> repository.save(null);
        Runnable findNull = () -> repository.findById(nullId);

        // Then
        assertThrows(InvalidArgumentException.class, saveNull::run);
        assertThrows(InvalidArgumentException.class, findNull::run);
    }
}
