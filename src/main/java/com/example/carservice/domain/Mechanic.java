package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.MechanicAlreadyAssignedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "mechanics")
public class Mechanic {
    private static String mechanicAlreadyAssignedMessage = "Mechanic is already assigned to an order.";

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialization", nullable = false, length = 32)
    private MechanicSpecialization specialization;

    @Column(name = "busy", nullable = false)
    private boolean busy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Mechanic() {
    }

    public Mechanic(String id, String fullName, MechanicSpecialization specialization) {
        if (id == null || id.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidMechanicId());
        }
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidMechanicName());
        }
        if (specialization == null) {
            throw new InvalidArgumentException(DomainMessages.specializationRequired());
        }
        this.id = id;
        this.fullName = fullName;
        this.specialization = specialization;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public MechanicSpecialization getSpecialization() {
        return specialization;
    }

    public boolean isBusy() {
        return busy;
    }

    public Long getVersion() {
        return version;
    }

    public void assignToOrder() {
        if (busy) {
            throw new MechanicAlreadyAssignedException(mechanicAlreadyAssignedMessage);
        }
        busy = true;
    }

    public void releaseFromOrder() {
        busy = false;
    }
}
