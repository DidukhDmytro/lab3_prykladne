package com.example.carservice.repository;

import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import java.util.List;
import java.util.Optional;

/**
 * Stores repair orders by identifier.
 *
 * <p>Saving an order with an existing identifier replaces the stored order. Lookup methods return
 * {@link Optional#empty()} when no matching order exists and never throw merely because an order
 * is missing. {@link #findAll()} and {@link #findByStatus(OrderStatus)} return unmodifiable lists
 * whose order is unspecified.</p>
 */
public interface RepairOrderRepository {
    RepairOrder save(RepairOrder order);

    Optional<RepairOrder> findById(String id);

    List<RepairOrder> findByStatus(OrderStatus status);

    List<RepairOrder> findAll();
}
