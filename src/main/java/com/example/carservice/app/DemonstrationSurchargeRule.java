package com.example.carservice.app;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.domain.enumerate.PricingPriority;
import java.math.BigDecimal;

public class DemonstrationSurchargeRule implements PricingRule {
    private BigDecimal amount;

    public DemonstrationSurchargeRule(BigDecimal amount) {
        if (amount == null) {
            throw new PricingException(PricingMessages.DEMONSTRATION_SURCHARGE_AMOUNT_REQUIRED);
        }
        this.amount = amount;
    }

    @Override
    public int getPriority() {
        return PricingPriority.SURCHARGE.getValue();
    }

    @Override
    public BigDecimal apply(PricingContext context, BigDecimal currentTotal) {
        if (context == null || currentTotal == null) {
            throw new PricingException(PricingMessages.PRICING_CONTEXT_REQUIRED);
        }
        return currentTotal.add(amount);
    }
}
