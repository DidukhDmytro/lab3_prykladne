package com.example.carservice.domain;

import java.math.BigDecimal;

public class Gift {
    private String name;
    private BigDecimal minimumPartsTotal;

    public Gift(String name, BigDecimal minimumPartsTotal) {
        this.name = name;
        this.minimumPartsTotal = minimumPartsTotal;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getMinimumPartsTotal() {
        return minimumPartsTotal;
    }
}
