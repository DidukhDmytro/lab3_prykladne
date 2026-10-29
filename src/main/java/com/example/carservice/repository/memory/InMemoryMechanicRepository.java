package com.example.carservice.repository.memory;

import com.example.carservice.domain.Mechanic;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.repository.MechanicRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryMechanicRepository implements MechanicRepository {
    private Map<String, Mechanic> mechanics = new HashMap<>();

    @Override
    public Mechanic save(Mechanic mechanic) {
        if (mechanic == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryMechanicRequired());
        }
        mechanics.put(mechanic.getId(), mechanic);
        return mechanic;
    }

    @Override
    public Optional<Mechanic> findById(String id) {
        if (id == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryIdentifierRequired());
        }
        return Optional.ofNullable(mechanics.get(id));
    }
}
