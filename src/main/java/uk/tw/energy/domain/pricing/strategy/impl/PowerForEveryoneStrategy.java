package uk.tw.energy.domain.pricing.strategy.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import uk.tw.energy.domain.pricing.BasePricingStrategy;

@Component
public class PowerForEveryoneStrategy extends BasePricingStrategy {

    @Override
    public String getPricePlanId() {
        return "price-plan-2";
    }

    @Override
    public String getProviderName() {
        return "Power for Everyone";
    }

    @Override
    public BigDecimal getBaseRate() {
        return new BigDecimal("1");
    }
}
