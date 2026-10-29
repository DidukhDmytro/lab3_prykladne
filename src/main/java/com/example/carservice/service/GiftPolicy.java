package com.example.carservice.service;

import com.example.carservice.config.AppConstants;
import com.example.carservice.domain.Gift;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.exception.DomainMessages;
import com.example.carservice.exception.InvalidArgumentException;
import java.math.BigDecimal;

public class GiftPolicy {
    public Present createPresent(RepairOrder order) {
        if (order == null) {
            throw new InvalidArgumentException(DomainMessages.giftOrderRequired());
        }
        BigDecimal partsTotal = order.calculatePartsTotal();
        return new Present(partsTotal, selectGift(partsTotal));
    }

    private Gift selectGift(BigDecimal partsTotal) {
        if (isAvailableFor(partsTotal, AppConstants.discountVoucherMinimum())) {
            return new Gift(AppConstants.discountVoucherName(), AppConstants.discountVoucherMinimum());
        }
        if (isAvailableFor(partsTotal, AppConstants.interiorCleaningMinimum())) {
            return new Gift(AppConstants.interiorCleaningName(), AppConstants.interiorCleaningMinimum());
        }
        if (isAvailableFor(partsTotal, AppConstants.washerFluidMinimum())) {
            return new Gift(AppConstants.washerFluidName(), AppConstants.washerFluidMinimum());
        }
        return null;
    }

    private boolean isAvailableFor(BigDecimal partsTotal, BigDecimal minimumPartsTotal) {
        return partsTotal.compareTo(minimumPartsTotal) >= 0;
    }
}
