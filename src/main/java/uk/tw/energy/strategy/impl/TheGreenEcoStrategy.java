package uk.tw.energy.strategy.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import uk.tw.energy.strategy.BasePricingStrategy;

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
