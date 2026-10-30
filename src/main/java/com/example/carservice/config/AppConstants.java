package com.example.carservice.config;

import java.math.BigDecimal;

public class AppConstants {
    public static String washerFluidName = "Washer fluid";
    public static String interiorCleaningName = "Interior cleaning";
    public static String discountVoucherName = "Discount voucher for your next visit";
    public static String noGiftLabel = "No gift";
    public static BigDecimal washerFluidMinimum = new BigDecimal("200");
    public static BigDecimal interiorCleaningMinimum = new BigDecimal("500");
    public static BigDecimal discountVoucherMinimum = new BigDecimal("1000");
    public static BigDecimal urgentLaborSurchargeFactor = new BigDecimal("0.20");
    public static BigDecimal minimumPrice = new BigDecimal("50.00");

    private AppConstants() {
    }
}
