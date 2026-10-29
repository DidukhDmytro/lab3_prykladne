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

/**
 * Registers customers, cars, and mechanics.
 */
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
        return carRepository.save(car);
    }

    /**
     * Stores a mechanic; an existing id is replaced.
     * @throws InvalidArgumentException if mechanic is null
     */
    public Mechanic registerMechanic(Mechanic mechanic) {
        return mechanicRepository.save(mechanic);
    }
}
