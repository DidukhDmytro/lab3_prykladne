package com.example.carservice.repository.memory;

import com.example.carservice.domain.Car;
import com.example.carservice.exception.EntityAlreadyExistsException;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.repository.CarRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryCarRepository implements CarRepository {
    private Map<String, Car> carsByVin = new HashMap<>();

    @Override
    public Car save(Car car) {
        if (car == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryCarRequired());
        }
        if (carsByVin.containsKey(car.getVin())) {
            throw new EntityAlreadyExistsException(
                    String.format(ServiceMessages.duplicateVin(), car.getVin()));
        }
        carsByVin.put(car.getVin(), car);
        return car;
    }

    @Override
    public Optional<Car> findByVin(String vin) {
        if (vin == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryIdentifierRequired());
        }
        return Optional.ofNullable(carsByVin.get(vin));
    }
}
