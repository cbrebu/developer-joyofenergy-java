package uk.tw.energy.domain.pricing.strategy;

import java.math.BigDecimal;
import java.util.List;
import uk.tw.energy.domain.electricity.ElectricityReading;

public interface PricingStrategy {
    String getPricePlanId();

    String getProviderName();

    BigDecimal calculateCost(List<ElectricityReading> readings);

    BigDecimal getBaseRate();
}
