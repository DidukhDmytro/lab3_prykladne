package com.example.carservice.domain;

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

@Entity
@Table(name = "repair_orders")
public class RepairOrder {
    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_vin", referencedColumnName = "vin", nullable = false)
    private Car car;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderColumn(name = "position")
    private List<ServiceWork> works;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mechanic_id")
    private Mechanic assignedMechanic;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
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

    public RepairOrder(String id, Car car) {
        if (id == null || id.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidRepairOrderId());
        }
        if (car == null) {
            throw new InvalidArgumentException(DomainMessages.carRequired());
        }
        this.id = id;
        this.car = car;
        this.works = new ArrayList<>();
        this.status = OrderStatus.CREATED;
        this.priority = OrderPriority.NORMAL;
    }

    public String getId() {
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
            throw new InvalidArgumentException(DomainMessages.priorityRequired());
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
            throw new InvalidArgumentException(DomainMessages.serviceWorkRequired());
        }
        if (assignedMechanic != null && !isCompatible(work, assignedMechanic)) {
            throw new MechanicNotCompatibleException(DomainMessages.mechanicNotCompatible());
        }
        work.assignToOrder(this);
        works.add(work);
    }

    public boolean removeWork(ServiceWork work) {
        ensureModifiable();
        boolean removed = works.remove(work);
        if (removed && !works.contains(work)) {
            work.assignToOrder(null);
        }
        return removed;
    }

    public void assignMechanic(Mechanic mechanic) {
        ensureModifiable();
        if (mechanic == null) {
            throw new InvalidArgumentException(DomainMessages.mechanicRequiredForAssignment());
        }
        if (!isCompatible(mechanic)) {
            throw new MechanicNotCompatibleException(DomainMessages.mechanicNotCompatible());
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
            throw new InvalidStateTransitionException(DomainMessages.invalidDiagnoseStatus());
        }
        status = OrderStatus.DIAGNOSED;
    }

    public void approve() {
        if (status != OrderStatus.DIAGNOSED) {
            throw new InvalidStateTransitionException(DomainMessages.invalidApproveStatus());
        }
        // Approval requires at least one work item.
        if (works.isEmpty()) {
            throw new InvalidStateTransitionException(DomainMessages.emptyOrder());
        }
        status = OrderStatus.APPROVED;
    }

    public void startProgress() {
        if (status != OrderStatus.APPROVED) {
            throw new InvalidStateTransitionException(DomainMessages.invalidProgressStatus());
        }
        // A mechanic must be assigned before work can begin.
        if (assignedMechanic == null) {
            throw new InvalidStateTransitionException(DomainMessages.mechanicRequired());
        }
        status = OrderStatus.IN_PROGRESS;
    }

    public void completeWork(int workIndex) {
        ensureModifiable();
        if (status != OrderStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(DomainMessages.invalidCompleteWorkStatus());
        }
        if (workIndex < 0 || workIndex >= works.size()) {
            throw new InvalidArgumentException(DomainMessages.workIndexOutOfRange());
        }
        works.get(workIndex).complete();
    }

    public void complete() {
        validateCompletion();
        status = OrderStatus.COMPLETED;
        assignedMechanic.releaseFromOrder();
    }

    public void complete(BigDecimal finalPrice, Instant completedAt) {
        if (finalPrice == null || completedAt == null) {
            throw new InvalidArgumentException(DomainMessages.completionDetailsRequired());
        }
        if (status == OrderStatus.IN_PROGRESS) {
            complete();
        } else if (status != OrderStatus.COMPLETED
                || this.finalPrice != null
                || this.completedAt != null) {
            throw new InvalidStateTransitionException(DomainMessages.invalidCompleteStatus());
        }
        this.finalPrice = finalPrice;
        this.completedAt = completedAt;
    }

    private void validateCompletion() {
        if (status != OrderStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(DomainMessages.invalidCompleteStatus());
        }
        if (works.stream().anyMatch(work -> work.getStatus() != WorkStatus.COMPLETED)) {
            throw new InvalidStateTransitionException(DomainMessages.unfinishedWork());
        }
    }

    public void cancel() {
        if (status != OrderStatus.CREATED
                && status != OrderStatus.DIAGNOSED
                && status != OrderStatus.APPROVED) {
            throw new InvalidStateTransitionException(DomainMessages.invalidCancelStatus() + status);
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
            throw new OrderModificationNotAllowedException(DomainMessages.closedOrder());
        }
    }

    private boolean isClosed() {
        return status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED;
    }
}
