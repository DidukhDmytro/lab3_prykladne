package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import com.example.carservice.exception.MechanicNotCompatibleException;
import com.example.carservice.exception.OrderModificationNotAllowedException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RepairOrder {
    private String id;
    private Car car;
    private List<ServiceWork> works;
    private Mechanic assignedMechanic;
    private OrderStatus status;
    private OrderPriority priority;

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

    public void changePriority(OrderPriority priority) {
        ensureModifiable();
        if (priority == null) {
            throw new InvalidArgumentException(DomainMessages.priorityRequired());
        }
        this.priority = priority;
    }

    public BigDecimal calculatePartsTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ServiceWork work : works) {
            total = total.add(work.getPartsCost());
        }
        return total;
    }

    public BigDecimal calculateLaborTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ServiceWork work : works) {
            total = total.add(work.getLaborCost());
        }
        return total;
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
        works.add(work);
    }

    public boolean removeWork(ServiceWork work) {
        ensureModifiable();
        return works.remove(work);
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
        if (status != OrderStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(DomainMessages.invalidCompleteStatus());
        }
        if (works.stream().anyMatch(work -> work.getStatus() != WorkStatus.COMPLETED)) {
            throw new InvalidStateTransitionException(DomainMessages.unfinishedWork());
        }
        status = OrderStatus.COMPLETED;
        assignedMechanic.releaseFromOrder();
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
