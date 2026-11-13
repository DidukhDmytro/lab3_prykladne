package com.example.carservice.pricing.rule;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.domain.enumerate.PricingPriority;
import java.math.BigDecimal;

public class UrgentSurchargeRule implements PricingRule {
    private static final BigDecimal ZERO_PRICE = BigDecimal.ZERO;
    private BigDecimal laborSurchargeFactor;

    public UrgentSurchargeRule(BigDecimal laborSurchargeFactor) {
        if (laborSurchargeFactor == null) {
            throw new PricingException(PricingMessages.URGENT_SURCHARGE_FACTOR_REQUIRED);
        }
        if (laborSurchargeFactor.compareTo(ZERO_PRICE) < 0) {
            throw new PricingException(PricingMessages.INVALID_URGENT_SURCHARGE_FACTOR);
        }
        this.laborSurchargeFactor = laborSurchargeFactor;
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
        if (context.isUrgent()) {
            return currentTotal.add(context.getLaborTotal().multiply(laborSurchargeFactor));
        }
        return currentTotal;
    }
}
