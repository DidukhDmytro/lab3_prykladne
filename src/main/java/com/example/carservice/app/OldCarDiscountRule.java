package com.example.carservice.app;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.pricing.rule.PercentageDiscountRule;
import java.math.BigDecimal;

public class OldCarDiscountRule implements PricingRule {
    private static int zeroYear = 0;
    private static int zeroAge = 0;

    private int currentYear;
    private int minimumAge;
    private PercentageDiscountRule percentageDiscountRule;

    public OldCarDiscountRule(int currentYear, int minimumAge, BigDecimal discountPercent) {
        if (currentYear <= zeroYear) {
            throw new PricingException(PricingMessages.oldCarCurrentYearRequired());
        }
        if (minimumAge < zeroAge) {
            throw new PricingException(PricingMessages.oldCarAgeLimitInvalid());
        }
        this.currentYear = currentYear;
        this.minimumAge = minimumAge;
        this.percentageDiscountRule = new PercentageDiscountRule(discountPercent);
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
        if (currentYear - context.getCarYear() > minimumAge) {
            return percentageDiscountRule.apply(context, currentTotal);
        }
        return currentTotal;
    }
}
