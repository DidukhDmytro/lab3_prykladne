package com.example.carservice.domain;

import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    protected Customer() {
    }

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
