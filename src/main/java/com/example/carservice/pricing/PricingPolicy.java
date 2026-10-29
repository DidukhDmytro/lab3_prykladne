package com.example.carservice.pricing;

import com.example.carservice.domain.RepairOrder;
import java.math.BigDecimal;

/**
 * Calculates repair-order prices.
 */
public interface PricingPolicy {
    /**
     * Calculates the base price without pricing rules.
     * @throws PricingException if the order is null or pricing inputs are invalid
     */
    BigDecimal calculateBasePrice(RepairOrder order);

    /**
     * Calculates the final price with pricing rules.
     * @throws PricingException if the order is null or pricing inputs are invalid
     */
    BigDecimal calculateFinalPrice(RepairOrder order);
}
