package com.example.carservice.domain.enumerate;

public enum PricingPriority {
    SURCHARGE(100),
    DISCOUNT(200);

    private int value;

    PricingPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
