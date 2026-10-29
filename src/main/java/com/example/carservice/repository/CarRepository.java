package com.example.carservice.repository;

import com.example.carservice.domain.Car;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Provides persistent access to cars by VIN.
 *
 * <p>Lookup returns {@link Optional#empty()} when the car is missing. Duplicate VINs are rejected
 * by the database primary-key constraint.</p>
 */
public interface CarRepository extends Repository<Car, String> {
    Car save(Car car);

    Optional<Car> findByVin(String vin);
}
