package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;

public class Car {
    private String vin;
    private String make;
    private String model;
    private int year;
    private Customer owner;

    public Car(String vin, String make, String model, int year, Customer owner) {
        if (vin == null || vin.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidVin());
        }
        if (owner == null) {
            throw new InvalidArgumentException(DomainMessages.ownerRequired());
        }
        this.vin = vin;
        this.make = make;
        this.model = model;
        this.year = year;
        this.owner = owner;
    }

    public String getVin() {
        return vin;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public Customer getOwner() {
        return owner;
    }
}
