package com.example.carservice.integration;

import com.example.carservice.config.AppConstants;
import com.example.carservice.domain.RepairOrder;
import com.example.carservice.pricing.PriceCalculator;
import com.example.carservice.pricing.PricingException;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.pricing.PricingRule;
import com.example.carservice.pricing.rule.UrgentSurchargeRule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
class PostgresIntegrationTestConfiguration {
    private static Instant fixedInstant = Instant.parse("2026-10-29T12:00:00Z");
    private static String forcedPricingFailureMessage = "Forced integration-test pricing failure.";

    @Bean
    @Primary
    Clock integrationTestClock() {
        return Clock.fixed(fixedInstant, ZoneOffset.UTC);
    }

    @Bean
    @Primary
    AdjustablePricingPolicy integrationTestPricingPolicy() {
        return new AdjustablePricingPolicy();
    }

    static class AdjustablePricingPolicy implements PricingPolicy {
        private PriceCalculator delegate;
        private boolean failCalculation;

        AdjustablePricingPolicy() {
            delegate = new PriceCalculator(
                    List.of(new UrgentSurchargeRule(AppConstants.urgentLaborSurchargeFactor)),
                    AppConstants.minimumPrice);
        }

        void setFailCalculation(boolean failCalculation) {
            this.failCalculation = failCalculation;
        }

        @Override
        public BigDecimal calculateBasePrice(RepairOrder order) {
            return delegate.calculateBasePrice(order);
        }

        @Override
        public BigDecimal calculateFinalPrice(RepairOrder order) {
            if (failCalculation) {
                throw new PricingException(forcedPricingFailureMessage);
            }
            return delegate.calculateFinalPrice(order);
        }
    }
}
