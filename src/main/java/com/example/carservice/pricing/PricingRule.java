package com.example.carservice.pricing;

import java.math.BigDecimal;

/**
 * A pricing operation ordered by priority.
 */
public interface PricingRule {
    int SURCHARGE_PRIORITY = 100;
    int DISCOUNT_PRIORITY = 200;

    /** Returns this rule's priority; lower values are applied first. */
    int getPriority();

    /**
     * Applies this rule to the pricing context and current total.
     * Both arguments must be non-null; invalid input raises {@link PricingException}.
     */
    BigDecimal apply(PricingContext context, BigDecimal currentTotal);
}
