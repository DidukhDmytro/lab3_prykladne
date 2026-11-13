package com.example.carservice.pricing;

public class PricingMessages {
    public static final String PRICING_RULES_REQUIRED = "Pricing rules are required.";
    public static final String PRICING_RULE_REQUIRED = "Pricing rules must not contain null.";
    public static final String MINIMUM_PRICE_REQUIRED = "Minimum price is required.";
    public static final String NEGATIVE_MINIMUM_PRICE = "Minimum price must not be negative.";
    public static final String PRICING_ORDER_REQUIRED = "A repair order is required for price calculation.";
    public static final String PRICING_CONTEXT_REQUIRED = "Pricing context is required.";
    public static final String PRICING_RULE_RESULT_REQUIRED = "Pricing rules must return a result.";
    public static final String LABOR_TOTAL_REQUIRED = "Labor total is required.";
    public static final String PARTS_TOTAL_REQUIRED = "Parts total is required.";
    public static final String NEGATIVE_LABOR_TOTAL = "Labor total must not be negative.";
    public static final String NEGATIVE_PARTS_TOTAL = "Parts total must not be negative.";
    public static final String URGENT_SURCHARGE_FACTOR_REQUIRED = "Urgent surcharge factor is required.";
    public static final String INVALID_URGENT_SURCHARGE_FACTOR = "Urgent surcharge factor must not be negative.";
    public static final String OLD_CAR_CURRENT_YEAR_REQUIRED = "Current year must be greater than zero.";
    public static final String OLD_CAR_AGE_LIMIT_INVALID = "Old-car age limit must not be negative.";
    public static final String PERCENTAGE_REQUIRED = "Discount percentage is required.";
    public static final String PERCENTAGE_OUT_OF_RANGE = "Discount percentage must be between zero and one hundred.";
    public static final String FIXED_DISCOUNT_REQUIRED = "Fixed discount amount is required.";
    public static final String NEGATIVE_FIXED_DISCOUNT = "Fixed discount amount must not be negative.";
    public static final String DEMONSTRATION_SURCHARGE_AMOUNT_REQUIRED =
            "Demonstration surcharge amount is required.";

    private PricingMessages() {
    }
}
