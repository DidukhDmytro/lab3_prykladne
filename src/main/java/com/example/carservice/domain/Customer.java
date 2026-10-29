package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;

public class Customer {
    private String id;
    private String fullName;
    private String phoneNumber;

    public Customer(String id, String fullName, String phoneNumber) {
        if (id == null || id.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidCustomerId());
        }
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidCustomerName());
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new InvalidArgumentException(DomainMessages.invalidPhoneNumber());
        }
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void updatePhoneNumber(String newPhoneNumber) {
        phoneNumber = newPhoneNumber;
    }
}
