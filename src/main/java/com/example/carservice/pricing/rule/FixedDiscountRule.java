package com.example.carservice.pricing.rule;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import java.math.BigDecimal;

public class FixedDiscountRule implements PricingRule {
    private static BigDecimal zeroPrice = BigDecimal.ZERO;

    private BigDecimal amount;

    public FixedDiscountRule(BigDecimal amount) {
        if (amount == null) {
            throw new PricingException(PricingMessages.fixedDiscountRequired());
        }
        if (amount.compareTo(zeroPrice) < 0) {
            throw new PricingException(PricingMessages.negativeFixedDiscount());
        }
        this.amount = amount;
    }

    @Override
    public int getPriority() {
        return DISCOUNT_PRIORITY;
    }

    @Override
    public BigDecimal apply(PricingContext context, BigDecimal currentTotal) {
        if (context == null || currentTotal == null) {
            throw new PricingException(PricingMessages.pricingContextRequired());
        }
        return currentTotal.subtract(amount).max(zeroPrice);
    }
}
