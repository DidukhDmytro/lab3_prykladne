package com.example.carservice.app;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import java.math.BigDecimal;

public class DemonstrationSurchargeRule implements PricingRule {
    private BigDecimal amount;

    public DemonstrationSurchargeRule(BigDecimal amount) {
        if (amount == null) {
            throw new PricingException(PricingMessages.demonstrationSurchargeAmountRequired());
        }
        this.amount = amount;
    }

    @Override
    public int getPriority() {
        return SURCHARGE_PRIORITY;
    }

    @Override
    public BigDecimal apply(PricingContext context, BigDecimal currentTotal) {
        if (context == null || currentTotal == null) {
            throw new PricingException(PricingMessages.pricingContextRequired());
        }
        return currentTotal.add(amount);
    }
}
