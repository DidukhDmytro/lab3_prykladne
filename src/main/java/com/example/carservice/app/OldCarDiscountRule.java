package com.example.carservice.app;

import com.example.carservice.pricing.PricingContext;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingMessages;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.domain.enumerate.PricingPriority;
import com.example.carservice.pricing.rule.PercentageDiscountRule;
import java.math.BigDecimal;

public class OldCarDiscountRule implements PricingRule {
    private static final int ZERO_YEAR = 0;
    private static final int ZERO_AGE = 0;

    private int currentYear;
    private int minimumAge;
    private PercentageDiscountRule percentageDiscountRule;

    public OldCarDiscountRule(int currentYear, int minimumAge, BigDecimal discountPercent) {
        if (currentYear <= ZERO_YEAR) {
            throw new PricingException(PricingMessages.OLD_CAR_CURRENT_YEAR_REQUIRED);
        }
        if (minimumAge < ZERO_AGE) {
            throw new PricingException(PricingMessages.OLD_CAR_AGE_LIMIT_INVALID);
        }
        this.currentYear = currentYear;
        this.minimumAge = minimumAge;
        this.percentageDiscountRule = new PercentageDiscountRule(discountPercent);
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
        if (currentYear - context.getCarYear() > minimumAge) {
            return percentageDiscountRule.apply(context, currentTotal);
        }
        return currentTotal;
    }
}
