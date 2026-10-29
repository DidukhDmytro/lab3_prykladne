package com.example.carservice.pricing;

import java.math.BigDecimal;

/**
 * Immutable pricing inputs with private fields, getters only, and no setters.
 */
public class PricingContext {
    private static BigDecimal zeroTotal = BigDecimal.ZERO;

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
            throw new PricingException(PricingMessages.laborTotalRequired());
        }
        if (laborTotal.compareTo(zeroTotal) < 0) {
            throw new PricingException(PricingMessages.negativeLaborTotal());
        }
        if (partsTotal == null) {
            throw new PricingException(PricingMessages.partsTotalRequired());
        }
        if (partsTotal.compareTo(zeroTotal) < 0) {
            throw new PricingException(PricingMessages.negativePartsTotal());
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
