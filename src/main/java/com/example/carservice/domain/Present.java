package com.example.carservice.domain;

import java.math.BigDecimal;
import java.util.Optional;

public class Present {
    private BigDecimal partsTotal;
    private Gift gift;

    public Present(BigDecimal partsTotal, Gift gift) {
        this.partsTotal = partsTotal;
        this.gift = gift;
    }

    public BigDecimal getPartsTotal() {
        return partsTotal;
    }

    public Optional<Gift> getGift() {
        return Optional.ofNullable(gift);
    }
}
