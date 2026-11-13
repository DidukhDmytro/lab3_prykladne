package com.example.carservice.service;

import com.example.carservice.config.AppConstants;
import com.example.carservice.domain.Gift;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class GiftPolicy {
    public Present createPresent(RepairOrder order) {
        if (order == null) {
            throw new InvalidArgumentException(DomainMessages.GIFT_ORDER_REQUIRED);
        }
        BigDecimal partsTotal = order.calculatePartsTotal();
        return new Present(partsTotal, selectGift(partsTotal));
    }

    private Gift selectGift(BigDecimal partsTotal) {
        if (isAvailableFor(partsTotal, AppConstants.DISCOUNT_VOUCHER_MINIMUM)) {
            return new Gift(AppConstants.DISCOUNT_VOUCHER_NAME, AppConstants.DISCOUNT_VOUCHER_MINIMUM);
        }
        if (isAvailableFor(partsTotal, AppConstants.INTERIOR_CLEANING_MINIMUM)) {
            return new Gift(AppConstants.INTERIOR_CLEANING_NAME, AppConstants.INTERIOR_CLEANING_MINIMUM);
        }
        if (isAvailableFor(partsTotal, AppConstants.WASHER_FLUID_MINIMUM)) {
            return new Gift(AppConstants.WASHER_FLUID_NAME, AppConstants.WASHER_FLUID_MINIMUM);
        }
        return null;
    }

    private boolean isAvailableFor(BigDecimal partsTotal, BigDecimal minimumPartsTotal) {
        return partsTotal.compareTo(minimumPartsTotal) >= 0;
    }
}
