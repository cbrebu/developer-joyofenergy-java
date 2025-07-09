package uk.tw.energy.strategy.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import uk.tw.energy.strategy.BasePricingStrategy;

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
