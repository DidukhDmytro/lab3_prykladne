package com.example.carservice.repository;

import com.example.carservice.domain.Car;
import java.util.Optional;

/**
 * Stores cars by VIN.
 *
 * <p>Saving a car whose VIN already exists throws an
 * {@link com.example.carservice.exception.EntityAlreadyExistsException}. Lookup returns
 * {@link Optional#empty()} when the VIN is not present and never throws merely because a car is
 * missing.</p>
 */
public interface CarRepository {
    Car save(Car car);

    Optional<Car> findByVin(String vin);
}
