package com.example.carservice.config;

import java.math.BigDecimal;

public class AppConstants {
    public static final String WASHER_FLUID_NAME = "Washer fluid";
    public static final String INTERIOR_CLEANING_NAME = "Interior cleaning";
    public static final String DISCOUNT_VOUCHER_NAME = "Discount voucher for your next visit";
    public static final BigDecimal WASHER_FLUID_MINIMUM = new BigDecimal("200");
    public static final BigDecimal INTERIOR_CLEANING_MINIMUM = new BigDecimal("500");
    public static final BigDecimal DISCOUNT_VOUCHER_MINIMUM = new BigDecimal("1000");
    public static final BigDecimal URGENT_LABOR_SURCHARGE_FACTOR = new BigDecimal("0.20");
    public static final BigDecimal MINIMUM_PRICE = new BigDecimal("50.00");

    private AppConstants() {
    }
}
