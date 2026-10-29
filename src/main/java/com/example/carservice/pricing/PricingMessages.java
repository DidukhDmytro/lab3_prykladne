package com.example.carservice.pricing;

public class PricingMessages {
    private static String pricingRulesRequired = "Pricing rules are required.";
    private static String pricingRuleRequired = "Pricing rules must not contain null.";
    private static String minimumPriceRequired = "Minimum price is required.";
    private static String negativeMinimumPrice = "Minimum price must not be negative.";
    private static String pricingOrderRequired = "A repair order is required for price calculation.";
    private static String pricingContextRequired = "Pricing context is required.";
    private static String pricingRuleResultRequired = "Pricing rules must return a result.";
    private static String laborTotalRequired = "Labor total is required.";
    private static String partsTotalRequired = "Parts total is required.";
    private static String negativeLaborTotal = "Labor total must not be negative.";
    private static String negativePartsTotal = "Parts total must not be negative.";
    private static String urgentSurchargeFactorRequired = "Urgent surcharge factor is required.";
    private static String invalidUrgentSurchargeFactor = "Urgent surcharge factor must not be negative.";
    private static String oldCarCurrentYearRequired = "Current year must be greater than zero.";
    private static String oldCarAgeLimitInvalid = "Old-car age limit must not be negative.";
    private static String percentageRequired = "Discount percentage is required.";
    private static String percentageOutOfRange = "Discount percentage must be between zero and one hundred.";
    private static String fixedDiscountRequired = "Fixed discount amount is required.";
    private static String negativeFixedDiscount = "Fixed discount amount must not be negative.";
    private static String demonstrationSurchargeAmountRequired =
            "Demonstration surcharge amount is required.";

    public static String pricingRulesRequired() { return pricingRulesRequired; }
    public static String pricingRuleRequired() { return pricingRuleRequired; }
    public static String minimumPriceRequired() { return minimumPriceRequired; }
    public static String negativeMinimumPrice() { return negativeMinimumPrice; }
    public static String pricingOrderRequired() { return pricingOrderRequired; }
    public static String pricingContextRequired() { return pricingContextRequired; }
    public static String pricingRuleResultRequired() { return pricingRuleResultRequired; }
    public static String laborTotalRequired() { return laborTotalRequired; }
    public static String partsTotalRequired() { return partsTotalRequired; }
    public static String negativeLaborTotal() { return negativeLaborTotal; }
    public static String negativePartsTotal() { return negativePartsTotal; }
    public static String urgentSurchargeFactorRequired() { return urgentSurchargeFactorRequired; }
    public static String invalidUrgentSurchargeFactor() { return invalidUrgentSurchargeFactor; }
    public static String oldCarCurrentYearRequired() { return oldCarCurrentYearRequired; }
    public static String oldCarAgeLimitInvalid() { return oldCarAgeLimitInvalid; }
    public static String percentageRequired() { return percentageRequired; }
    public static String percentageOutOfRange() { return percentageOutOfRange; }
    public static String fixedDiscountRequired() { return fixedDiscountRequired; }
    public static String negativeFixedDiscount() { return negativeFixedDiscount; }
    public static String demonstrationSurchargeAmountRequired() {
        return demonstrationSurchargeAmountRequired;
    }

    private PricingMessages() {
    }
}
