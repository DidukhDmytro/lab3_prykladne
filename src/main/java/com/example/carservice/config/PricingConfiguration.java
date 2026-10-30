package com.example.carservice.config;

import com.example.carservice.pricing.PriceCalculator;
import com.example.carservice.pricing.PricingPolicy;
import com.example.carservice.pricing.rule.UrgentSurchargeRule;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingConfiguration {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public PricingPolicy pricingPolicy() {
        return new PriceCalculator(
                List.of(new UrgentSurchargeRule(AppConstants.urgentLaborSurchargeFactor)),
                AppConstants.minimumPrice);
    }
}
