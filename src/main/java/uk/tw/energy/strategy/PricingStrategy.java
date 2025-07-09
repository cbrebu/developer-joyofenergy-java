package uk.tw.energy.strategy;

import java.math.BigDecimal;
import java.util.List;
import uk.tw.energy.domain.ElectricityReading;

public interface PricingStrategy {
    String getPricePlanId();

    String getProviderName();

    BigDecimal calculateCost(List<ElectricityReading> readings);

    BigDecimal getBaseRate();
}
