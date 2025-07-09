// src/main/java/uk/tw/energy/domain/PricePlan.java
package uk.tw.energy.domain.pricing;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public record PricePlan(
        String planId, String energySupplier, BigDecimal unitRate, List<PeakTimeMultiplier> peakTimeMultipliers) {

    public BigDecimal getPrice(LocalDateTime dateTime) {
        if (peakTimeMultipliers != null) {
            for (PeakTimeMultiplier multiplier : peakTimeMultipliers) {
                if (multiplier.dayOfWeek() == dateTime.getDayOfWeek()) {
                    return unitRate.multiply(multiplier.multiplier());
                }
            }
        }
        return unitRate;
    }

    public record PeakTimeMultiplier(DayOfWeek dayOfWeek, BigDecimal multiplier) {}
}
