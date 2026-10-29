package com.example.carservice.pricing.rule;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import java.math.BigDecimal;

public class UrgentSurchargeRule implements PricingRule {
    private static BigDecimal zeroPrice = BigDecimal.ZERO;
    private BigDecimal laborSurchargeFactor;

    public UrgentSurchargeRule(BigDecimal laborSurchargeFactor) {
        if (laborSurchargeFactor == null) {
            throw new PricingException(PricingMessages.urgentSurchargeFactorRequired());
        }
        if (laborSurchargeFactor.compareTo(zeroPrice) < 0) {
            throw new PricingException(PricingMessages.invalidUrgentSurchargeFactor());
        }
        this.laborSurchargeFactor = laborSurchargeFactor;
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
        if (context.isUrgent()) {
            return currentTotal.add(context.getLaborTotal().multiply(laborSurchargeFactor));
        }
        return currentTotal;
    }
}
