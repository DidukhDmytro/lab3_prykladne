package com.example.carservice.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.carservice.config.AppConstants;
import com.example.carservice.domain.Car;
import com.example.carservice.domain.Customer;
import com.example.carservice.domain.MechanicSpecialization;
import com.example.carservice.domain.OrderPriority;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.domain.ServiceWork;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.pricing.rule.PercentageDiscountRule;
import com.example.carservice.pricing.rule.UrgentSurchargeRule;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriceCalculatorTest {
    private String id = "order-pricing";
    private String vin = "VIN-PRICING";
    private String description = "Pricing work";
    private RepairOrder order;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("customer-pricing", "Pricing Customer", "555-0100");
        Car car = new Car(vin, "Test", "Model", 2020, customer);
        order = new RepairOrder(id, car);
    }

    @Test
    @DisplayName("Calculates zero base price for an empty order")
    void calculatesEmptyBaseTotal() {
        // Given
        PriceCalculator calculator = new PriceCalculator(List.of(), new BigDecimal("50.00"));

        // When
        BigDecimal price = calculator.calculateBasePrice(order);

        // Then
        assertEquals(new BigDecimal("0.00"), price);
    }

    @Test
    @DisplayName("Calculates the minimum final price for an empty order")
    void calculatesMinimumFinalPriceForEmptyOrder() {
        // Given
        BigDecimal minimumPrice = new BigDecimal("50.00");
        PriceCalculator calculator = new PriceCalculator(List.of(), minimumPrice);

        // When
        BigDecimal price = calculator.calculateFinalPrice(order);

        // Then
        assertEquals(minimumPrice, price);
    }

    @Test
    @DisplayName("Applies custom tax priority after discounts")
    void appliesTaxPriorityAfterDiscounts() {
        // Given
        addWork("100.00", "50.00");
        PriceCalculator calculator = new PriceCalculator(
                List.of(new PercentageDiscountRule(new BigDecimal("10")), new TaxRule()),
                BigDecimal.ZERO);

        // When
        BigDecimal price = calculator.calculateFinalPrice(order);

        // Then
        assertEquals(new BigDecimal("148.50"), price);
    }

    @Test
    @DisplayName("Keeps insertion order for equal-priority rules")
    void keepsInsertionOrderForEqualPriorityRules() {
        // Given
        addWork("100.00", "50.00");
        PriceCalculator multiplyThenAdd = new PriceCalculator(
                List.of(new MultiplyRule(), new AddAmountRule()),
                BigDecimal.ZERO);
        PriceCalculator addThenMultiply = new PriceCalculator(
                List.of(new AddAmountRule(), new MultiplyRule()),
                BigDecimal.ZERO);

        // When
        BigDecimal firstResult = multiplyThenAdd.calculateFinalPrice(order);
        BigDecimal secondResult = addThenMultiply.calculateFinalPrice(order);

        // Then
        assertEquals(new BigDecimal("301.00"), firstResult);
        assertEquals(new BigDecimal("302.00"), secondResult);
    }

    @Test
    @DisplayName("Rejects a negative minimum price")
    void rejectsNegativeMinimumPrice() {
        // Given
        BigDecimal minimumPrice = new BigDecimal("-0.01");

        // When
        PricingException exception = assertThrows(
                PricingException.class,
                () -> new PriceCalculator(List.of(), minimumPrice));

        // Then
        assertEquals(PricingMessages.negativeMinimumPrice(), exception.getMessage());
    }

    @ParameterizedTest(name = "parts {0}, labor {1}, then parts {2}, labor {3} gives {4}")
    @CsvSource({
            "0.00,0.00,0.00,0.00,0.00",
            "10.00,5.00,0.00,0.00,15.00",
            "10.00,5.00,20.00,3.00,38.00"
    })
    @DisplayName("Calculates base totals for zero, one, and multiple works")
    void calculatesBaseTotal(String firstParts, String firstLabor, String secondParts,
                             String secondLabor, String expected) {
        // Given
        addWork(firstParts, firstLabor);
        if (new BigDecimal(secondParts).compareTo(BigDecimal.ZERO) > 0
                || new BigDecimal(secondLabor).compareTo(BigDecimal.ZERO) > 0) {
            addWork(secondParts, secondLabor);
        }
        PriceCalculator calculator = new PriceCalculator(List.of(), BigDecimal.ZERO);

        // When
        BigDecimal price = calculator.calculateBasePrice(order);

        // Then
        assertEquals(new BigDecimal(expected), price);
    }

    @Test
    @DisplayName("Final price equals base price when there are no rules")
    void calculatesFinalPriceWithoutRules() {
        // Given
        addWork("40.00", "12.00");
        PriceCalculator calculator = new PriceCalculator(List.of(), BigDecimal.ZERO);

        // When
        BigDecimal price = calculator.calculateFinalPrice(order);

        // Then
        assertEquals(new BigDecimal("52.00"), price);
    }

    @Test
    @DisplayName("Urgent surcharge applies only to labor")
    void urgentSurchargeAppliesOnlyToLabor() {
        // Given
        addWork("100.00", "50.00");
        order.changePriority(OrderPriority.URGENT);
        PriceCalculator calculator =
                new PriceCalculator(
                        List.of(new UrgentSurchargeRule(AppConstants.urgentLaborSurchargeFactor())),
                        BigDecimal.ZERO);

        // When
        BigDecimal price = calculator.calculateFinalPrice(order);

        // Then
        assertEquals(new BigDecimal("160.00"), price);
    }

    @ParameterizedTest(name = "base total {0} resolves to {1} with minimum 50.00")
    @CsvSource({
            "50.00,50.00",
            "49.99,50.00",
            "50.01,50.01"
    })
    @DisplayName("Applies minimum price at exact and neighboring values")
    void appliesMinimumPrice(String amount, String expected) {
        // Given
        addWork(amount, "0.00");
        PriceCalculator calculator = new PriceCalculator(List.of(), new BigDecimal("50.00"));

        // When
        BigDecimal price = calculator.calculateFinalPrice(order);

        // Then
        assertEquals(new BigDecimal(expected), price);
    }

    @Test
    @DisplayName("Rounds calculated price once using HALF_UP at scale two")
    void roundsHalfUpAtScaleTwo() {
        // Given
        addWork("1.005", "0.00");
        PriceCalculator calculator = new PriceCalculator(List.of(), BigDecimal.ZERO);

        // When
        BigDecimal price = calculator.calculateFinalPrice(order);

        // Then
        assertEquals(new BigDecimal("1.01"), price);
    }

    private void addWork(String parts, String labor) {
        order.addWork(new ServiceWork(
                description,
                new BigDecimal(parts),
                new BigDecimal(labor),
                MechanicSpecialization.GENERAL));
    }

    private class TaxRule implements PricingRule {
        private BigDecimal taxRate = new BigDecimal("0.10");

        @Override
        public int getPriority() {
            return 300;
        }

        @Override
        public BigDecimal apply(PricingContext context, BigDecimal currentTotal) {
            return currentTotal.add(currentTotal.multiply(taxRate));
        }
    }

    private class MultiplyRule implements PricingRule {
        private BigDecimal multiplier = new BigDecimal("2");

        @Override
        public int getPriority() {
            return PricingRule.SURCHARGE_PRIORITY;
        }

        @Override
        public BigDecimal apply(PricingContext context, BigDecimal currentTotal) {
            return currentTotal.multiply(multiplier);
        }
    }

    private class AddAmountRule implements PricingRule {
        private BigDecimal amount = BigDecimal.ONE;

        @Override
        public int getPriority() {
            return PricingRule.SURCHARGE_PRIORITY;
        }

        @Override
        public BigDecimal apply(PricingContext context, BigDecimal currentTotal) {
            return currentTotal.add(amount);
        }
    }
}
