package com.example.carservice.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies portable pricing rules to immutable pricing context data.
 *
 * <p>Inputs must include a non-null context, a non-null rule list with no null elements, and a
 * minimum price greater than or equal to zero. Rules must return non-null amounts. The engine
 * rejects invalid inputs with {@link PricingException} and never returns {@code null}.</p>
 *
 * <p>The final-price result has scale two and uses {@link RoundingMode#HALF_UP}, rounded exactly
 * once at the end. Negative rule results are clamped to zero before the minimum price is applied,
 * so the final result is never below the minimum price. Base-price calculation returns the
 * context totals without applying rules or the minimum price, rounded to scale two.</p>
 *
 * <p>The engine does not mutate input data and defensively copies and sorts the rule list once
 * during construction; equal-priority rules retain the order in which they were passed (stable
 * sort).</p>
 *
 * <p>The engine is stateless. Repeated calls with equal contexts return equal results when all
 * rules are stateless and deterministic.</p>
 */
public class PricingRuleEngine {
    private static int priceScale = 2;
    private static BigDecimal zeroPrice = BigDecimal.ZERO;

    private List<PricingRule> rules;
    private BigDecimal minimumPrice;

    public PricingRuleEngine(List<PricingRule> rules, BigDecimal minimumPrice) {
        if (rules == null) {
            throw new PricingException(PricingMessages.pricingRulesRequired());
        }
        if (minimumPrice == null) {
            throw new PricingException(PricingMessages.minimumPriceRequired());
        }
        if (minimumPrice.compareTo(zeroPrice) < 0) {
            throw new PricingException(PricingMessages.negativeMinimumPrice());
        }
        this.rules = new ArrayList<>(rules);
        if (this.rules.contains(null)) {
            throw new PricingException(PricingMessages.pricingRuleRequired());
        }
        this.rules.sort(Comparator.comparingInt(PricingRule::getPriority));
        this.minimumPrice = minimumPrice;
    }

    public BigDecimal calculateBasePrice(PricingContext context) {
        validateContext(context);
        return context.getPartsTotal()
                .add(context.getLaborTotal())
                .setScale(priceScale, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateFinalPrice(PricingContext context) {
        validateContext(context);
        BigDecimal total = context.getPartsTotal().add(context.getLaborTotal());
        for (PricingRule rule : rules) {
            total = rule.apply(context, total);
            if (total == null) {
                throw new PricingException(PricingMessages.pricingRuleResultRequired());
            }
        }
        return total.max(zeroPrice).max(minimumPrice).setScale(priceScale, RoundingMode.HALF_UP);
    }

    private void validateContext(PricingContext context) {
        if (context == null) {
            throw new PricingException(PricingMessages.pricingContextRequired());
        }
    }
}
