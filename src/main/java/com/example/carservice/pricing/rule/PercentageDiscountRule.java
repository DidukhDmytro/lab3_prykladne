package com.example.carservice.pricing.rule;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.domain.enumerate.PricingPriority;
import java.math.BigDecimal;

public class PercentageDiscountRule implements PricingRule {
    private static final BigDecimal ZERO_PERCENT = BigDecimal.ZERO;
    private static final BigDecimal MAXIMUM_PERCENT = new BigDecimal("100");
    private static final BigDecimal PERCENTAGE_DIVISOR = new BigDecimal("100");

    private BigDecimal percent;

    public PercentageDiscountRule(BigDecimal percent) {
        if (percent == null) {
            throw new PricingException(PricingMessages.PERCENTAGE_REQUIRED);
        }
        if (percent.compareTo(ZERO_PERCENT) < 0 || percent.compareTo(MAXIMUM_PERCENT) > 0) {
            throw new PricingException(PricingMessages.PERCENTAGE_OUT_OF_RANGE);
        }
        this.percent = percent;
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
        return currentTotal.multiply(PERCENTAGE_DIVISOR.subtract(percent)).divide(PERCENTAGE_DIVISOR);
    }
}
