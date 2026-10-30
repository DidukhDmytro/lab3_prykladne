package com.example.carservice.pricing;

public class PricingMessages {
    public static String pricingRulesRequired = "Pricing rules are required.";
    public static String pricingRuleRequired = "Pricing rules must not contain null.";
    public static String minimumPriceRequired = "Minimum price is required.";
    public static String negativeMinimumPrice = "Minimum price must not be negative.";
    public static String pricingOrderRequired = "A repair order is required for price calculation.";
    public static String pricingContextRequired = "Pricing context is required.";
    public static String pricingRuleResultRequired = "Pricing rules must return a result.";
    public static String laborTotalRequired = "Labor total is required.";
    public static String partsTotalRequired = "Parts total is required.";
    public static String negativeLaborTotal = "Labor total must not be negative.";
    public static String negativePartsTotal = "Parts total must not be negative.";
    public static String urgentSurchargeFactorRequired = "Urgent surcharge factor is required.";
    public static String invalidUrgentSurchargeFactor = "Urgent surcharge factor must not be negative.";
    public static String oldCarCurrentYearRequired = "Current year must be greater than zero.";
    public static String oldCarAgeLimitInvalid = "Old-car age limit must not be negative.";
    public static String percentageRequired = "Discount percentage is required.";
    public static String percentageOutOfRange = "Discount percentage must be between zero and one hundred.";
    public static String fixedDiscountRequired = "Fixed discount amount is required.";
    public static String negativeFixedDiscount = "Fixed discount amount must not be negative.";
    public static String demonstrationSurchargeAmountRequired =
            "Demonstration surcharge amount is required.";

    private PricingMessages() {
    }
}
