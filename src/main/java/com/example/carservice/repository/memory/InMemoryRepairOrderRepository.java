package com.example.carservice.repository.memory;

import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.ServiceMessages;
import com.example.carservice.repository.RepairOrderRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryRepairOrderRepository implements RepairOrderRepository {
    private Map<String, RepairOrder> orders = new HashMap<>();

    @Override
    public RepairOrder save(RepairOrder order) {
        if (order == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryOrderRequired);
        }
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<RepairOrder> findById(String id) {
        if (id == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryIdentifierRequired);
        }
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<RepairOrder> findByStatus(OrderStatus status) {
        if (status == null) {
            throw new InvalidArgumentException(ServiceMessages.repositoryStatusRequired);
        }
        return Collections.unmodifiableList(orders.values().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList()));
    }

    @Override
    public List<RepairOrder> findAll() {
        return Collections.unmodifiableList(orders.values().stream()
                .collect(Collectors.toList()));
    }
}
