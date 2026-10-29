package com.example.carservice.service;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Mechanic;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.EntityNotFoundException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.repository.CarRepository;
import com.example.carservice.repository.CustomerRepository;
import com.example.carservice.repository.MechanicRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers customers, cars, and mechanics.
 */
@Service
public class RegistrationService {
    private CarRepository carRepository;
    private MechanicRepository mechanicRepository;
    private CustomerRepository customerRepository;

    /**
     * Creates a service using the supplied repositories.
     * @throws InvalidArgumentException if any repository is null
     */
    public RegistrationService(
            CustomerRepository customerRepository,
            CarRepository carRepository,
            MechanicRepository mechanicRepository) {
        if (customerRepository == null || carRepository == null || mechanicRepository == null) {
            throw new InvalidArgumentException(ServiceMessages.serviceDependenciesRequired());
        }
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.mechanicRepository = mechanicRepository;
    }

    /**
     * Finds a customer by identifier.
     * @param customerId customer identifier
     * @return the customer if it exists
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findCustomer(String customerId) {
        requireIdentifier(customerId);
        return customerRepository.findById(customerId);
    }

    /**
     * Finds a car by VIN.
     * @param vin vehicle identification number
     * @return the car if it exists
     */
    @Transactional(readOnly = true)
    public Optional<Car> findCar(String vin) {
        requireIdentifier(vin);
        return carRepository.findByVin(vin);
    }

    /**
     * Finds a mechanic by identifier.
     * @param mechanicId mechanic identifier
     * @return the mechanic if it exists
     */
    @Transactional(readOnly = true)
    public Optional<Mechanic> findMechanic(String mechanicId) {
        requireIdentifier(mechanicId);
        return mechanicRepository.findById(mechanicId);
    }

    /**
     * Registers a customer if its id is not already stored.
     * @throws InvalidArgumentException if customer is null; EntityAlreadyExistsException if its id exists
     */
    public Customer registerCustomer(Customer customer) {
        if (customer == null) {
            throw new InvalidArgumentException(ServiceMessages.entityRequired());
        }
        if (customerRepository.findById(customer.getId()).isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.duplicateEntity(), customer.getId()));
        }
        return customerRepository.save(customer);
    }

    /**
     * Registers a car whose owner is already registered.
     * @throws InvalidArgumentException if car is null; EntityNotFoundException if its owner is missing; EntityAlreadyExistsException if its VIN exists
     */
    public Car registerCar(Car car) {
        if (car == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryCarRequired());
        }
        String customerId = car.getOwner().getId();
        customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.customerNotFound(), customerId)));
        if (carRepository.findByVin(car.getVin()).isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.duplicateVin(), car.getVin()));
        }
        return carRepository.save(car);
    }

    /**
     * Stores a mechanic; an existing id is replaced.
     * @throws InvalidArgumentException if mechanic is null
     */
    public Mechanic registerMechanic(Mechanic mechanic) {
        if (mechanic == null) {
            throw new InvalidArgumentException(ServiceMessages.serviceMechanicRequired());
        }
        return mechanicRepository.save(mechanic);
    }

    private void requireIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidArgumentException(ServiceMessages.serviceIdentifierRequired());
        }
    }
}
