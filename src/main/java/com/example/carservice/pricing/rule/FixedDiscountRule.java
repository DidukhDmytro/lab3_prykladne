package com.example.carservice.pricing.rule;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.domain.enumerate.PricingPriority;
import java.math.BigDecimal;

public class FixedDiscountRule implements PricingRule {
    private static final BigDecimal ZERO_PRICE = BigDecimal.ZERO;

    private BigDecimal amount;

    public FixedDiscountRule(BigDecimal amount) {
        if (amount == null) {
            throw new PricingException(PricingMessages.FIXED_DISCOUNT_REQUIRED);
        }
        if (amount.compareTo(ZERO_PRICE) < 0) {
            throw new PricingException(PricingMessages.NEGATIVE_FIXED_DISCOUNT);
        }
        this.amount = amount;
    }

    @Override
    public int getPriority() {
        return PricingPriority.DISCOUNT.getValue();
    }

    @Override
    public BigDecimal apply(PricingContext context, BigDecimal currentTotal) {
        if (context == null || currentTotal == null) {
            throw new PricingException(PricingMessages.PRICING_CONTEXT_REQUIRED);
        }
        return currentTotal.subtract(amount).max(ZERO_PRICE);
    }
}
