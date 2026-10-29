package com.example.carservice.pricing;

import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import java.math.BigDecimal;
import java.util.List;

/**
 * Adapts repair orders to the portable pricing rule engine.
 */
public class PriceCalculator implements PricingPolicy {
    private PricingRuleEngine pricingRuleEngine;

    public PriceCalculator(List<PricingRule> rules, BigDecimal minimumPrice) {
        pricingRuleEngine = new PricingRuleEngine(rules, minimumPrice);
    }

    @Override
    public BigDecimal calculateBasePrice(RepairOrder order) {
        return pricingRuleEngine.calculateBasePrice(toPricingContext(order));
    }

    @Override
    public BigDecimal calculateFinalPrice(RepairOrder order) {
        return pricingRuleEngine.calculateFinalPrice(toPricingContext(order));
    }

    private PricingContext toPricingContext(RepairOrder order) {
        if (order == null) {
            throw new PricingException(PricingMessages.pricingOrderRequired());
        }
        return new PricingContext(
                order.calculateLaborTotal(),
                order.calculatePartsTotal(),
                order.getPriority() == OrderPriority.URGENT,
                order.getCar().getYear());
    }
}
