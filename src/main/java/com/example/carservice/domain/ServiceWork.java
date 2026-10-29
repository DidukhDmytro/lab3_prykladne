package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "service_works")
public class ServiceWork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "parts_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal partsCost;

    @Column(name = "labor_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal laborCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_specialization", nullable = false, length = 32)
    private MechanicSpecialization requiredSpecialization;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private WorkStatus status;

    protected ServiceWork() {
    }

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
