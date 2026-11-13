package com.example.carservice.domain;

import com.example.carservice.domain.enumerate.MechanicSpecialization;
import com.example.carservice.domain.enumerate.OrderPriority;
import com.example.carservice.domain.enumerate.OrderStatus;
import com.example.carservice.domain.enumerate.WorkStatus;
import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "repair_orders")
public class RepairOrder {
    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_vin", referencedColumnName = "vin", nullable = false)
    private Car car;

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderColumn(name = "position", nullable = false)
    private List<ServiceWork> works;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id")
    private Mechanic assignedMechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private OrderPriority priority;

    @Column(name = "final_price", precision = 12, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected RepairOrder() {
        works = new ArrayList<>();
    }

    public RepairOrder(UUID id, Car car) {
        if (id == null) {
            throw new InvalidArgumentException(DomainMessages.INVALID_REPAIR_ORDER_ID);
        }
        if (car == null) {
            throw new InvalidArgumentException(DomainMessages.CAR_REQUIRED);
        }
        this.id = id;
        this.car = car;
        this.works = new ArrayList<>();
        this.status = OrderStatus.CREATED;
        this.priority = OrderPriority.NORMAL;
    }

    public UUID getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public List<ServiceWork> getWorks() {
        return Collections.unmodifiableList(works);
    }

    public Mechanic getAssignedMechanic() {
        return assignedMechanic;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public OrderPriority getPriority() {
        return priority;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * Returns the optimistic-lock version used to detect concurrent updates.
     */
    public Long getVersion() {
        return version;
    }

    public void changePriority(OrderPriority priority) {
        ensureModifiable();
        if (priority == null) {
            throw new InvalidArgumentException(DomainMessages.PRIORITY_REQUIRED);
        }
        this.priority = priority;
    }

    public BigDecimal calculatePartsTotal() {
        return works.stream()
                .map(ServiceWork::getPartsCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateLaborTotal() {
        return works.stream()
                .map(ServiceWork::getLaborCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateBaseTotal() {
        return calculatePartsTotal().add(calculateLaborTotal());
    }

    public void addWork(ServiceWork work) {
        ensureModifiable();
        if (work == null) {
            throw new InvalidArgumentException(DomainMessages.SERVICE_WORK_REQUIRED);
        }
        if (assignedMechanic != null && !isCompatible(work, assignedMechanic)) {
            throw new MechanicNotCompatibleException(DomainMessages.MECHANIC_NOT_COMPATIBLE);
        }
        works.add(work);
    }

    public boolean removeWork(ServiceWork work) {
        ensureModifiable();
        return works.remove(work);
    }

    public void assignMechanic(Mechanic mechanic) {
        ensureModifiable();
        if (mechanic == null) {
            throw new InvalidArgumentException(DomainMessages.MECHANIC_REQUIRED_FOR_ASSIGNMENT);
        }
        if (!isCompatible(mechanic)) {
            throw new MechanicNotCompatibleException(DomainMessages.MECHANIC_NOT_COMPATIBLE);
        }
        if (Objects.equals(mechanic, assignedMechanic)) {
            return;
        }
        mechanic.assignToOrder();
        if (assignedMechanic != null) {
            assignedMechanic.releaseFromOrder();
        }
        assignedMechanic = mechanic;
    }

    public void diagnose() {
        if (status != OrderStatus.CREATED) {
            throw new InvalidStateTransitionException(DomainMessages.INVALID_DIAGNOSE_STATUS);
        }
        status = OrderStatus.DIAGNOSED;
    }

    public void approve() {
        if (status != OrderStatus.DIAGNOSED) {
            throw new InvalidStateTransitionException(DomainMessages.INVALID_APPROVE_STATUS);
        }
        // Approval requires at least one work item.
        if (works.isEmpty()) {
            throw new InvalidStateTransitionException(DomainMessages.EMPTY_ORDER);
        }
        status = OrderStatus.APPROVED;
    }

    public void startProgress() {
        if (status != OrderStatus.APPROVED) {
            throw new InvalidStateTransitionException(DomainMessages.INVALID_PROGRESS_STATUS);
        }
        // A mechanic must be assigned before work can begin.
        if (assignedMechanic == null) {
            throw new InvalidStateTransitionException(DomainMessages.MECHANIC_REQUIRED);
        }
        status = OrderStatus.IN_PROGRESS;
    }

    public void completeWork(int workIndex) {
        ensureModifiable();
        if (status != OrderStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(DomainMessages.INVALID_COMPLETE_WORK_STATUS);
        }
        if (workIndex < 0 || workIndex >= works.size()) {
            throw new InvalidArgumentException(DomainMessages.WORK_INDEX_OUT_OF_RANGE);
        }
        works.get(workIndex).complete();
    }

    public void complete(BigDecimal finalPrice, Instant completedAt) {
        if (finalPrice == null || completedAt == null) {
            throw new InvalidArgumentException(DomainMessages.COMPLETION_DETAILS_REQUIRED);
        }
        ensureCanComplete();
        status = OrderStatus.COMPLETED;
        assignedMechanic.releaseFromOrder();
        this.finalPrice = finalPrice;
        this.completedAt = completedAt;
    }

    public void ensureCanComplete() {
        if (status != OrderStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(DomainMessages.INVALID_COMPLETE_STATUS);
        }
        if (works.stream().anyMatch(work -> work.getStatus() != WorkStatus.COMPLETED)) {
            throw new InvalidStateTransitionException(DomainMessages.UNFINISHED_WORK);
        }
    }

    public void cancel() {
        if (status != OrderStatus.CREATED
                && status != OrderStatus.DIAGNOSED
                && status != OrderStatus.APPROVED) {
            throw new InvalidStateTransitionException(DomainMessages.INVALID_CANCEL_STATUS + status);
        }
        status = OrderStatus.CANCELLED;
        if (assignedMechanic != null) {
            assignedMechanic.releaseFromOrder();
        }
    }

    private boolean isCompatible(Mechanic mechanic) {
        return works.stream().allMatch(work -> isCompatible(work, mechanic));
    }

    private boolean isCompatible(ServiceWork work, Mechanic mechanic) {
        return work.getRequiredSpecialization() == MechanicSpecialization.GENERAL
                || mechanic.getSpecialization() == MechanicSpecialization.GENERAL
                || work.getRequiredSpecialization() == mechanic.getSpecialization();
    }

    private void ensureModifiable() {
        // Completed and cancelled orders cannot be changed.
        if (isClosed()) {
            throw new OrderModificationNotAllowedException(DomainMessages.CLOSED_ORDER);
        }
    }

    private boolean isClosed() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED;
    }
}
