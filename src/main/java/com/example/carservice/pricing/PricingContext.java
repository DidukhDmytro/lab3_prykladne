package com.example.carservice.pricing;

import java.math.BigDecimal;

/**
 * Immutable pricing inputs with private fields, getters only, and no setters.
 */
public class PricingContext {
    private static final BigDecimal ZERO_TOTAL = BigDecimal.ZERO;

    private BigDecimal laborTotal;
    private BigDecimal partsTotal;
    private boolean urgent;
    private int carYear;

    public PricingContext(
            BigDecimal laborTotal,
            BigDecimal partsTotal,
            boolean urgent,
            int carYear) {
        if (laborTotal == null) {
            throw new PricingException(PricingMessages.LABOR_TOTAL_REQUIRED);
        }
        if (laborTotal.compareTo(ZERO_TOTAL) < 0) {
            throw new PricingException(PricingMessages.NEGATIVE_LABOR_TOTAL);
        }
        if (partsTotal == null) {
            throw new PricingException(PricingMessages.PARTS_TOTAL_REQUIRED);
        }
        if (partsTotal.compareTo(ZERO_TOTAL) < 0) {
            throw new PricingException(PricingMessages.NEGATIVE_PARTS_TOTAL);
        }
        this.laborTotal = laborTotal;
        this.partsTotal = partsTotal;
        this.urgent = urgent;
        this.carYear = carYear;
    }

    public BigDecimal getLaborTotal() {
        return laborTotal;
    }

    public BigDecimal getPartsTotal() {
        return partsTotal;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public int getCarYear() {
        return carYear;
    }
}
