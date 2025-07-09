package uk.tw.energy.domain.pricing.strategy.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import uk.tw.energy.domain.pricing.BasePricingStrategy;

@Component
public class TheGreenEcoStrategy extends BasePricingStrategy {
    @Override
    public String getPricePlanId() {
        return "price-plan-1";
    }

    @Override
    public String getProviderName() {
        return "The Green Eco";
    }

    @Override
    public BigDecimal getBaseRate() {
        return new BigDecimal("2");
    }
}
