package com.example.carservice.repository;

import com.example.carservice.domain.OrderStatus;
import com.example.carservice.domain.RepairOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistent access to repair orders.
 *
 * <p>Lookup returns {@link Optional#empty()} when an order is missing. Fetch methods load the
 * associated work and repair-order references needed by callers.</p>
 */
public interface RepairOrderRepository extends Repository<RepairOrder, String> {
    RepairOrder save(RepairOrder order);

    @EntityGraph(attributePaths = {"works", "car", "car.owner", "assignedMechanic"})
    Optional<RepairOrder> findById(String id);

    @Query("select distinct repairOrder from RepairOrder repairOrder "
            + "left join fetch repairOrder.works "
            + "left join fetch repairOrder.car car "
            + "left join fetch car.owner "
            + "left join fetch repairOrder.assignedMechanic "
            + "where repairOrder.status = :status")
    List<RepairOrder> findByStatus(@Param("status") OrderStatus status);

    @Query("select distinct repairOrder from RepairOrder repairOrder "
            + "left join fetch repairOrder.works "
            + "left join fetch repairOrder.car car "
            + "left join fetch car.owner")
    List<RepairOrder> findAll();
}
