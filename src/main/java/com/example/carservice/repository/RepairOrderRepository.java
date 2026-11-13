package com.example.carservice.repository;

import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistent access to repair orders.
 *
 * <p>Lookup returns {@link Optional#empty()} when an order is missing. Fetch methods load the
 * associated work and repair-order references needed by callers.</p>
 */
public interface RepairOrderRepository extends JpaRepository<RepairOrder, UUID> {

    @EntityGraph(attributePaths = {"works", "car", "car.owner", "assignedMechanic"})
    Optional<RepairOrder> findById(UUID id);

    @Query("""
            select repairOrder from RepairOrder repairOrder
            left join fetch repairOrder.works
            left join fetch repairOrder.car car
            left join fetch car.owner
            left join fetch repairOrder.assignedMechanic
            where repairOrder.status = :status
            """)
    List<RepairOrder> findByStatus(@Param("status") OrderStatus status);

    @Query("""
            select repairOrder from RepairOrder repairOrder
            left join fetch repairOrder.works
            left join fetch repairOrder.car car
            left join fetch car.owner
            """)
    List<RepairOrder> findAll();
}
