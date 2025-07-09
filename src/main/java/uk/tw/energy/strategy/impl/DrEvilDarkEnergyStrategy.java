package uk.tw.energy.strategy.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import uk.tw.energy.strategy.BasePricingStrategy;

@Component
public class DrEvilDarkEnergyStrategy extends BasePricingStrategy {

    @Override
    public String getPricePlanId() {
        return "price-plan-0";
    }

    @Override
    public String getProviderName() {
        return "Dr Evil's Dark Energy";
    }

    @Override
    public BigDecimal getBaseRate() {
        return new BigDecimal("10");
    }
}
