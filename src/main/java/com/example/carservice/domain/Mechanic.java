package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import com.example.carservice.exception.MechanicAlreadyAssignedException;

public class Mechanic {
    private static String mechanicAlreadyAssignedMessage = "Mechanic is already assigned to an order.";

    private String id;
    private String fullName;
    private MechanicSpecialization specialization;
    private boolean busy;

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
