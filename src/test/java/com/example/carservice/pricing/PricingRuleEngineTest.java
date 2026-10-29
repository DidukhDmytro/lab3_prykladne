package com.example.carservice.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.carservice.pricing.rule.FixedDiscountRule;
import com.example.carservice.pricing.rule.PercentageDiscountRule;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PricingRuleEngineTest {
    private static BigDecimal sampleLaborTotal = new BigDecimal("50.00");
    private static BigDecimal samplePartsTotal = new BigDecimal("100.00");
    private static BigDecimal negativeTotal = new BigDecimal("-0.01");
    private static BigDecimal standaloneDiscountPercent = new BigDecimal("10");
    private static BigDecimal standaloneExpectedPrice = new BigDecimal("135.00");
    private static int sampleCarYear = 2020;

    private PricingContext context;

    @BeforeEach
    void setUp() {
        context = new PricingContext(sampleLaborTotal, samplePartsTotal, false, sampleCarYear);
    }

    @Test
    @DisplayName("Runs directly with a standalone pricing context")
    void runsWithoutRepairOrder() {
        // Given
        PricingContext standaloneContext =
                new PricingContext(sampleLaborTotal, samplePartsTotal, false, sampleCarYear);
        PricingRuleEngine engine = new PricingRuleEngine(
                List.of(new PercentageDiscountRule(standaloneDiscountPercent)),
                BigDecimal.ZERO);

        // When
        BigDecimal result = engine.calculateFinalPrice(standaloneContext);

        // Then
        assertEquals(standaloneExpectedPrice, result);
    }

    @Test
    @DisplayName("Rejects a null pricing context")
    void rejectsNullContext() {
        // Given
        PricingRuleEngine engine = new PricingRuleEngine(List.of(), BigDecimal.ZERO);

        // When
        Runnable calculate = () -> engine.calculateFinalPrice(null);

        // Then
        PricingException exception =
                assertThrows(PricingException.class, calculate::run);
        assertEquals(PricingMessages.pricingContextRequired(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a null rule list")
    void rejectsNullRuleList() {
        // Given
        List<PricingRule> rules = null;

        // When
        Runnable createEngine = () -> new PricingRuleEngine(rules, BigDecimal.ZERO);

        // Then
        PricingException exception =
                assertThrows(PricingException.class, createEngine::run);
        assertEquals(PricingMessages.pricingRulesRequired(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects null elements in the rule list")
    void rejectsNullRuleElement() {
        // Given
        List<PricingRule> rules = new ArrayList<>();
        rules.add(null);

        // When
        Runnable createEngine = () -> new PricingRuleEngine(rules, BigDecimal.ZERO);

        // Then
        PricingException exception =
                assertThrows(PricingException.class, createEngine::run);
        assertEquals(PricingMessages.pricingRuleRequired(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a negative minimum price")
    void rejectsNegativeMinimumPrice() {
        // Given
        BigDecimal minimumPrice = new BigDecimal("-0.01");

        // When
        Runnable createEngine = () -> new PricingRuleEngine(List.of(), minimumPrice);

        // Then
        PricingException exception =
                assertThrows(PricingException.class, createEngine::run);
        assertEquals(PricingMessages.negativeMinimumPrice(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a null minimum price")
    void rejectsNullMinimumPrice() {
        // Given
        BigDecimal minimumPrice = null;

        // When
        Runnable createEngine = () -> new PricingRuleEngine(List.of(), minimumPrice);

        // Then
        PricingException exception =
                assertThrows(PricingException.class, createEngine::run);
        assertEquals(PricingMessages.minimumPriceRequired(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a rule that returns null")
    void rejectsNullRuleResult() {
        // Given
        PricingRuleEngine engine =
                new PricingRuleEngine(List.of(new NullResultRule()), BigDecimal.ZERO);

        // When
        Runnable calculate = () -> engine.calculateFinalPrice(context);

        // Then
        PricingException exception =
                assertThrows(PricingException.class, calculate::run);
        assertEquals(PricingMessages.pricingRuleResultRequired(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a null labor total in a pricing context")
    void rejectsNullLaborTotal() {
        // When
        Runnable createContext = () ->
                new PricingContext(null, BigDecimal.ZERO, false, sampleCarYear);

        // Then
        PricingException exception = assertThrows(PricingException.class, createContext::run);
        assertEquals(PricingMessages.laborTotalRequired(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a null parts total in a pricing context")
    void rejectsNullPartsTotal() {
        // When
        Runnable createContext = () ->
                new PricingContext(BigDecimal.ZERO, null, false, sampleCarYear);

        // Then
        PricingException exception = assertThrows(PricingException.class, createContext::run);
        assertEquals(PricingMessages.partsTotalRequired(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a negative labor total in a pricing context")
    void rejectsNegativeLaborTotal() {
        // When
        Runnable createContext = () ->
                new PricingContext(negativeTotal, BigDecimal.ZERO, false, sampleCarYear);

        // Then
        PricingException exception = assertThrows(PricingException.class, createContext::run);
        assertEquals(PricingMessages.negativeLaborTotal(), exception.getMessage());
    }

    @Test
    @DisplayName("Rejects a negative parts total in a pricing context")
    void rejectsNegativePartsTotal() {
        // When
        Runnable createContext = () ->
                new PricingContext(BigDecimal.ZERO, negativeTotal, false, sampleCarYear);

        // Then
        PricingException exception = assertThrows(PricingException.class, createContext::run);
        assertEquals(PricingMessages.negativePartsTotal(), exception.getMessage());
    }

    @Test
    @DisplayName("Returns equal results for repeated calls with the same context")
    void repeatedCallsReturnEqualResults() {
        // Given
        PricingRuleEngine engine = new PricingRuleEngine(
                List.of(new PercentageDiscountRule(new BigDecimal("10"))),
                BigDecimal.ZERO);

        // When
        BigDecimal firstResult = engine.calculateFinalPrice(context);
        BigDecimal secondResult = engine.calculateFinalPrice(context);

        // Then
        assertEquals(firstResult, secondResult);
    }

    @Test
    @DisplayName("Does not mutate context or the original rule list")
    void doesNotMutateInputs() {
        // Given
        BigDecimal laborTotal = context.getLaborTotal();
        BigDecimal partsTotal = context.getPartsTotal();
        boolean urgent = context.isUrgent();
        int carYear = context.getCarYear();
        List<PricingRule> rules = new ArrayList<>(List.of(new AddOneRule()));
        PricingRuleEngine engine = new PricingRuleEngine(rules, BigDecimal.ZERO);
        rules.clear();

        // When
        BigDecimal result = engine.calculateFinalPrice(context);

        // Then
        assertEquals(new BigDecimal("151.00"), result);
        assertTrue(rules.isEmpty());
        assertEquals(laborTotal, context.getLaborTotal());
        assertEquals(partsTotal, context.getPartsTotal());
        assertEquals(urgent, context.isUrgent());
        assertEquals(carYear, context.getCarYear());
    }

    @Test
    @DisplayName("Rounds only once at the end using HALF_UP")
    void roundsOnlyOnceAtHalfUpBoundary() {
        // Given
        PricingRuleEngine engine = new PricingRuleEngine(
                List.of(
                        new ExactAmountRule(new BigDecimal("1.005")),
                        new MultiplyAmountRule(new BigDecimal("2"))),
                BigDecimal.ZERO);

        // When
        BigDecimal result = engine.calculateFinalPrice(context);

        // Then
        assertEquals(new BigDecimal("2.01"), result);
    }

    @Test
    @DisplayName("Clamps negative rule output before applying the minimum")
    void neverReturnsBelowMinimum() {
        // Given
        BigDecimal minimumPrice = new BigDecimal("10.00");
        PricingRuleEngine engine =
                new PricingRuleEngine(List.of(new ExactAmountRule(new BigDecimal("-5.00"))), minimumPrice);

        // When
        BigDecimal result = engine.calculateFinalPrice(context);

        // Then
        assertEquals(minimumPrice, result);
    }

    @Test
    @DisplayName("Fixed and percentage discount order changes the result")
    void fixedAndPercentageDiscountsAreOrderSensitive() {
        // Given
        PricingRuleEngine fixedThenPercent = new PricingRuleEngine(
                List.of(
                        new FixedDiscountRule(new BigDecimal("20")),
                        new PercentageDiscountRule(new BigDecimal("10"))),
                BigDecimal.ZERO);
        PricingRuleEngine percentThenFixed = new PricingRuleEngine(
                List.of(
                        new PercentageDiscountRule(new BigDecimal("10")),
                        new FixedDiscountRule(new BigDecimal("20"))),
                BigDecimal.ZERO);

        // When
        BigDecimal firstResult = fixedThenPercent.calculateFinalPrice(context);
        BigDecimal secondResult = percentThenFixed.calculateFinalPrice(context);

        // Then
        assertEquals(new BigDecimal("117.00"), firstResult);
        assertEquals(new BigDecimal("115.00"), secondResult);
    }

    @Test
    @DisplayName("Two percentage discounts commute")
    void percentageDiscountsAreCommutative() {
        // Given
        PricingRuleEngine firstOrder = new PricingRuleEngine(
                List.of(
                        new PercentageDiscountRule(new BigDecimal("10")),
                        new PercentageDiscountRule(new BigDecimal("20"))),
                BigDecimal.ZERO);
        PricingRuleEngine reverseOrder = new PricingRuleEngine(
                List.of(
                        new PercentageDiscountRule(new BigDecimal("20")),
                        new PercentageDiscountRule(new BigDecimal("10"))),
                BigDecimal.ZERO);

        // When
        BigDecimal firstResult = firstOrder.calculateFinalPrice(context);
        BigDecimal secondResult = reverseOrder.calculateFinalPrice(context);

        // Then
        assertEquals(new BigDecimal("108.00"), firstResult);
        assertEquals(firstResult, secondResult);
    }

    private class NullResultRule implements PricingRule {
        @Override
        public int getPriority() {
            return PricingRule.SURCHARGE_PRIORITY;
        }

        @Override
        public BigDecimal apply(PricingContext ruleContext, BigDecimal currentTotal) {
            return null;
        }
    }

    private class AddOneRule implements PricingRule {
        @Override
        public int getPriority() {
            return PricingRule.SURCHARGE_PRIORITY;
        }

        @Override
        public BigDecimal apply(PricingContext ruleContext, BigDecimal currentTotal) {
            return currentTotal.add(BigDecimal.ONE);
        }
    }

    private class ExactAmountRule implements PricingRule {
        private BigDecimal amount;

        private ExactAmountRule(BigDecimal amount) {
            this.amount = amount;
        }

        @Override
        public int getPriority() {
            return PricingRule.SURCHARGE_PRIORITY;
        }

        @Override
        public BigDecimal apply(PricingContext ruleContext, BigDecimal currentTotal) {
            return amount;
        }
    }

    private class MultiplyAmountRule implements PricingRule {
        private BigDecimal multiplier;

        private MultiplyAmountRule(BigDecimal multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public int getPriority() {
            return PricingRule.DISCOUNT_PRIORITY;
        }

        @Override
        public BigDecimal apply(PricingContext ruleContext, BigDecimal currentTotal) {
            return currentTotal.multiply(multiplier);
        }
    }
}
