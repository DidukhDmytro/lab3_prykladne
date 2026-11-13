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
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistrationService {
    private final CarRepository carRepository;
    private final MechanicRepository mechanicRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Optional<Customer> findCustomer(UUID customerId) {
        requireIdentifier(customerId);
        return customerRepository.findById(customerId);
    }

    @Transactional(readOnly = true)
    public Optional<Car> findCar(String vin) {
        requireIdentifier(vin);
        return carRepository.findByVin(vin);
    }


    @Transactional(readOnly = true)
    public Optional<Mechanic> findMechanic(UUID mechanicId) {
        requireIdentifier(mechanicId);
        return mechanicRepository.findById(mechanicId);
    }


    public Customer registerCustomer(Customer customer) {
        if (customer == null) {
            throw new InvalidArgumentException(ServiceMessages.ENTITY_REQUIRED);
        }
        if (customerRepository.findById(customer.getId()).isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.DUPLICATE_ENTITY, customer.getId()));
        }
        return customerRepository.save(customer);
    }

    public Car registerCar(Car car) {
        if (car == null) {
            throw new InvalidArgumentException(ServiceMessages.REPOSITORY_CAR_REQUIRED);
        }
        UUID customerId = car.getOwner().getId();
        customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ServiceMessages.CUSTOMER_NOT_FOUND, customerId)));
        if (carRepository.findByVin(car.getVin()).isPresent()) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.DUPLICATE_VIN, car.getVin()));
        }
        return carRepository.save(car);
    }


    public Mechanic registerMechanic(Mechanic mechanic) {
        if (mechanic == null) {
            throw new InvalidArgumentException(ServiceMessages.SERVICE_MECHANIC_REQUIRED);
        }
        return mechanicRepository.save(mechanic);
    }

    private void requireIdentifier(UUID identifier) {
        if (identifier == null) {
            throw new InvalidArgumentException(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED);
        }
    }

    private void requireIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidArgumentException(ServiceMessages.SERVICE_IDENTIFIER_REQUIRED);
        }
    }
}
