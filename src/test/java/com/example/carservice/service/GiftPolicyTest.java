package com.example.carservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.Gift;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.Present;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GiftPolicyTest {
    private String workDescription = "Gift threshold work";
    private String noGift = "No gift";
    private RepairOrder order;
    private GiftPolicy giftPolicy;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("customer-gift", "Gift Customer", "555-0100");
        Car car = new Car("VIN-GIFT", "Test", "Model", 2020, customer);
        order = new RepairOrder("order-gift", car);
        giftPolicy = new GiftPolicy();
    }

    @ParameterizedTest(name = "parts total {0} receives {1}")
    @CsvSource({
            "199.99,No gift",
            "200.00,Washer fluid",
            "499.99,Washer fluid",
            "500.00,Interior cleaning",
            "999.99,Interior cleaning",
            "1000.00,Discount voucher for your next visit"
    })
    @DisplayName("Selects the expected gift at tier boundaries")
    void selectsGiftAtTierBoundaries(String parts, String expectedGift) {
        // Given
        addParts(parts);

        // When
        Optional<Gift> gift = giftPolicy.createPresent(order).getGift();

        // Then
        assertEquals(expectedGift, gift.map(Gift::getName).orElse(noGift));
    }

    @Test
    @DisplayName("Returns no gift for zero parts total")
    void returnsNoGiftForZeroParts() {
        // Given
        addParts("0.00");

        // When
        Present present = giftPolicy.createPresent(order);

        // Then
        assertEquals(0, present.getPartsTotal().compareTo(BigDecimal.ZERO));
        assertEquals(noGift, present.getGift().map(Gift::getName).orElse(noGift));
    }

    private void addParts(String amount) {
        order.addWork(new ServiceWork(
                workDescription,
                new BigDecimal(amount),
                BigDecimal.ZERO,
                MechanicSpecialization.GENERAL));
    }
}
