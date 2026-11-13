package com.example.carservice.repository;

import com.example.carservice.domain.Car;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistent access to cars by VIN.
 *
 * <p>Lookup returns {@link Optional#empty()} when the car is missing. Duplicate VINs are rejected
 * by the database primary-key constraint.</p>
 */
public interface CarRepository extends JpaRepository<Car, String> {
    Optional<Car> findByVin(String vin);
}
