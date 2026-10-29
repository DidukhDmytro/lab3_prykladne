package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import java.math.BigDecimal;

public class ServiceWork {
    private String description;
    private BigDecimal partsCost;
    private BigDecimal laborCost;
    private MechanicSpecialization requiredSpecialization;
    private WorkStatus status;

    public ServiceWork(
            String description,
            BigDecimal partsCost,
            BigDecimal laborCost,
            MechanicSpecialization requiredSpecialization) {
        if (description == null || description.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidServiceWorkDescription());
        }
        if (partsCost == null || laborCost == null) {
            throw new InvalidArgumentException(DomainMessages.costsRequired());
        }
        if (partsCost.compareTo(BigDecimal.ZERO) < 0 || laborCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidArgumentException(DomainMessages.costsMustBeNonNegative());
        }
        if (requiredSpecialization == null) {
            throw new InvalidArgumentException(DomainMessages.specializationRequired());
        }
        this.description = description;
        this.partsCost = partsCost;
        this.laborCost = laborCost;
        this.requiredSpecialization = requiredSpecialization;
        this.status = WorkStatus.PENDING;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPartsCost() {
        return partsCost;
    }

    public BigDecimal getLaborCost() {
        return laborCost;
    }

    public MechanicSpecialization getRequiredSpecialization() {
        return requiredSpecialization;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public void complete() {
        if (status == WorkStatus.COMPLETED) {
            throw new InvalidStateTransitionException(DomainMessages.workAlreadyCompleted());
        }
        status = WorkStatus.COMPLETED;
    }
}
