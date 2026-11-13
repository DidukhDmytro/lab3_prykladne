package com.example.carservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.CustomerRepository;
import com.example.carservice.repository.MechanicRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    private static final UUID MECHANIC_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000101");
    private static final String MECHANIC_NAME = "Registration Mechanic";
    private static final UUID MISSING_MECHANIC_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000102");
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final String CUSTOMER_NAME = "Registration Customer";
    private static final String CUSTOMER_PHONE = "555-0100";
    private static final String VIN = "VIN-REGISTRATION";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    @InjectMocks
    private RegistrationService service;
    private Customer customer;
    private Car car;

    @BeforeEach
    void setUp() {
        customer = new Customer(CUSTOMER_ID, CUSTOMER_NAME, CUSTOMER_PHONE);
        car = new Car(VIN, "Test", "Model", 2020, customer);
    }

    @Test
    @DisplayName("Rejects registering a duplicate customer")
    void rejectsDuplicateCustomer() {
        // Given
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        // When
        EntityAlreadyExistsException exception =
                assertThrows(EntityAlreadyExistsException.class, () -> service.registerCustomer(customer));

        // Then
        assertEquals(
                String.format(ServiceMessages.DUPLICATE_ENTITY, CUSTOMER_ID),
                exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejects registering a car with an unknown owner")
    void rejectsCarWithUnknownOwner() {
        // Given
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception =
                assertThrows(EntityNotFoundException.class, () -> service.registerCar(car));

        // Then
        assertEquals(String.format(ServiceMessages.CUSTOMER_NOT_FOUND, CUSTOMER_ID), exception.getMessage());
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejects registering a car with an existing VIN")
    void rejectsDuplicateCarVin() {
        // Given
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(carRepository.findByVin(VIN)).thenReturn(Optional.of(car));

        // When
        EntityAlreadyExistsException exception =
                assertThrows(EntityAlreadyExistsException.class, () -> service.registerCar(car));

        // Then
        assertEquals(String.format(ServiceMessages.DUPLICATE_VIN, VIN), exception.getMessage());
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registers a customer and a car with that owner")
    void registersCustomerAndOwnedCar() {
        // Given
        when(customerRepository.findById(CUSTOMER_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(carRepository.save(car)).thenReturn(car);

        // When
        Customer registeredCustomer = service.registerCustomer(customer);
        Car registeredCar = service.registerCar(car);

        // Then
        assertSame(customer, registeredCustomer);
        assertSame(car, registeredCar);
        verify(customerRepository).save(customer);
        verify(carRepository).save(car);
    }

    @Test
    @DisplayName("Finds a customer by identifier")
    void findsCustomer() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        assertEquals(Optional.of(customer), service.findCustomer(CUSTOMER_ID));
        verify(customerRepository).findById(CUSTOMER_ID);
    }

    @Test
    @DisplayName("Returns empty when a customer is missing")
    void returnsEmptyForMissingCustomer() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.findCustomer(CUSTOMER_ID));
    }

    @Test
    @DisplayName("Finds a car by VIN")
    void findsCar() {
        when(carRepository.findByVin(VIN)).thenReturn(Optional.of(car));

        assertEquals(Optional.of(car), service.findCar(VIN));
        verify(carRepository).findByVin(VIN);
    }

    @Test
    @DisplayName("Returns empty when a car is missing")
    void returnsEmptyForMissingCar() {
        when(carRepository.findByVin(VIN)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.findCar(VIN));
    }

    @Test
    @DisplayName("Finds a mechanic by identifier")
    void findsMechanic() {
        Mechanic mechanic = new Mechanic(MECHANIC_ID, MECHANIC_NAME, MechanicSpecialization.GENERAL);
        when(mechanicRepository.findById(mechanic.getId())).thenReturn(Optional.of(mechanic));

        assertEquals(Optional.of(mechanic), service.findMechanic(mechanic.getId()));
        verify(mechanicRepository).findById(mechanic.getId());
    }

    @Test
    @DisplayName("Returns empty when a mechanic is missing")
    void returnsEmptyForMissingMechanic() {
        when(mechanicRepository.findById(MISSING_MECHANIC_ID)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), service.findMechanic(MISSING_MECHANIC_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "CAR", "MECHANIC"})
    @DisplayName("Rejects null lookup identifiers before repository access")
    void rejectsNullLookupIdentifiers(String lookup) {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> performLookupWithNullIdentifier(lookup));

        assertEquals(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED, exception.getMessage());
        verifyNoInteractions(customerRepository, carRepository, mechanicRepository);
    }

    @Test
    @DisplayName("Rejects registering a null mechanic before repository access")
    void rejectsNullMechanicBeforeRepositoryAccess() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> service.registerMechanic(null));

        assertEquals(ServiceMessages.SERVICE_MECHANIC_REQUIRED, exception.getMessage());
        verifyNoInteractions(customerRepository, carRepository, mechanicRepository);
    }

    private void performLookupWithNullIdentifier(String lookup) {
        switch (lookup) {
            case "CUSTOMER" -> service.findCustomer(null);
            case "CAR" -> service.findCar(null);
            case "MECHANIC" -> service.findMechanic(null);
        }
    }
}
