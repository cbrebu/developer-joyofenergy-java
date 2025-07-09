package uk.tw.energy.strategy.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import uk.tw.energy.strategy.BasePricingStrategy;

@Component
public class DraculaEnergyStrategy extends BasePricingStrategy {

    @Override
    public String getPricePlanId() {
        return "price-plan-3";
    }

    @Override
    public String getProviderName() {
        return "Dracula Romania Energy";
    }

    @Override
    public BigDecimal getBaseRate() {
        return new BigDecimal("50");
    }
}
