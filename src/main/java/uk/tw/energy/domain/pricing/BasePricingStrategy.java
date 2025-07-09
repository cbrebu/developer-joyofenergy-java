package uk.tw.energy.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import uk.tw.energy.domain.electricity.ElectricityReading;
import uk.tw.energy.domain.pricing.strategy.PricingStrategy;

public abstract class BasePricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculateCost(List<ElectricityReading> readings) {
        if (readings == null || readings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (readings.size() == 1) {
            return readings.getFirst().reading().multiply(getBaseRate());
        }

        BigDecimal averageReadingInKw = calculateAverageReading(readings);
        BigDecimal usageTimeInHours = calculateUsageTimeInHours(readings);
        BigDecimal energyConsumedInKwH = averageReadingInKw.divide(usageTimeInHours, RoundingMode.HALF_UP);

        return energyConsumedInKwH.multiply(getBaseRate()).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageReading(List<ElectricityReading> readings) {
        BigDecimal summedReadings =
                readings.stream().map(ElectricityReading::reading).reduce(BigDecimal.ZERO, BigDecimal::add);

        return summedReadings.divide(BigDecimal.valueOf(readings.size()), RoundingMode.HALF_UP);
    }

    private BigDecimal calculateUsageTimeInHours(List<ElectricityReading> readings) {
        ElectricityReading first = readings.stream()
                .min(Comparator.comparing(ElectricityReading::time))
                .orElseThrow();

        ElectricityReading last = readings.stream()
                .max(Comparator.comparing(ElectricityReading::time))
                .orElseThrow();

        long seconds = last.time().getEpochSecond() - first.time().getEpochSecond();
        return BigDecimal.valueOf(seconds).divide(BigDecimal.valueOf(3600), 10, RoundingMode.HALF_UP);
    }
}
