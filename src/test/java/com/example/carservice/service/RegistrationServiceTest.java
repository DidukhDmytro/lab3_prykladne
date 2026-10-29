package com.example.carservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.CustomerRepository;
import com.example.carservice.repository.MechanicRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    private String customerId = "customer-registration";
    private String customerName = "Registration Customer";
    private String customerPhone = "555-0100";
    private String vin = "VIN-REGISTRATION";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private MechanicRepository mechanicRepository;

    private RegistrationService service;
    private Customer customer;
    private Car car;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(customerRepository, carRepository, mechanicRepository);
        customer = new Customer(customerId, customerName, customerPhone);
        car = new Car(vin, "Test", "Model", 2020, customer);
    }

    @Test
    @DisplayName("Rejects registering a duplicate customer")
    void rejectsDuplicateCustomer() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // When
        EntityAlreadyExistsException exception =
                assertThrows(EntityAlreadyExistsException.class, () -> service.registerCustomer(customer));

        // Then
        assertEquals(
                String.format(ServiceMessages.duplicateEntity(), customerId),
                exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejects registering a car with an unknown owner")
    void rejectsCarWithUnknownOwner() {
        // Given
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When
        EntityNotFoundException exception =
                assertThrows(EntityNotFoundException.class, () -> service.registerCar(car));

        // Then
        assertEquals(String.format(ServiceMessages.customerNotFound(), customerId), exception.getMessage());
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registers a customer and a car with that owner")
    void registersCustomerAndOwnedCar() {
        // Given
        when(customerRepository.findById(customerId))
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
}
