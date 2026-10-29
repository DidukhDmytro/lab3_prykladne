package com.example.carservice.config;

import java.math.BigDecimal;

public class AppConstants {
    private static String washerFluidName = "Washer fluid";
    private static String interiorCleaningName = "Interior cleaning";
    private static String discountVoucherName = "Discount voucher for your next visit";
    private static String noGiftLabel = "No gift";
    private static BigDecimal washerFluidMinimum = new BigDecimal("200");
    private static BigDecimal interiorCleaningMinimum = new BigDecimal("500");
    private static BigDecimal discountVoucherMinimum = new BigDecimal("1000");
    private static BigDecimal urgentLaborSurchargeFactor = new BigDecimal("0.20");
    private static BigDecimal minimumPrice = new BigDecimal("50.00");

    public static String washerFluidName() { return washerFluidName; }
    public static String interiorCleaningName() { return interiorCleaningName; }
    public static String discountVoucherName() { return discountVoucherName; }
    public static String noGiftLabel() { return noGiftLabel; }
    public static BigDecimal washerFluidMinimum() { return washerFluidMinimum; }
    public static BigDecimal interiorCleaningMinimum() { return interiorCleaningMinimum; }
    public static BigDecimal discountVoucherMinimum() { return discountVoucherMinimum; }
    public static BigDecimal urgentLaborSurchargeFactor() { return urgentLaborSurchargeFactor; }
    public static BigDecimal minimumPrice() { return minimumPrice; }

    private AppConstants() {
    }
}
