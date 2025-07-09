package uk.tw.energy.application.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.electricity.ElectricityReading;
import uk.tw.energy.domain.pricing.strategy.PricingStrategy;
import uk.tw.energy.domain.pricing.strategy.PricingStrategyFactory;

@Service
@AllArgsConstructor
public class PricePlanService {

    private final PricingStrategyFactory strategyFactory;
    private final MeterReadingService meterReadingService;
    private final AccountService accountService;

    public void assignPricePlanToMeter(String smartMeterId, String pricePlanId) {
        if (!meterExists(smartMeterId)) {
            throw new IllegalArgumentException("Smart Meter ID does not exist: " + smartMeterId);
        }

        if (!strategyFactory.hasPricePlan(pricePlanId)) {
            throw new IllegalArgumentException("Price Plan ID does not exist: " + pricePlanId);
        }

        accountService.setPricePlanForMeter(smartMeterId, pricePlanId);
    }

    private boolean meterExists(String smartMeterId) {
        return accountService.hasMeter(smartMeterId);
    }

    public Optional<Map<String, BigDecimal>> calculateConsumptionCostsForAllPricePlans(String smartMeterId) {

        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (electricityReadings.isEmpty() || electricityReadings.get().isEmpty()) {
            return Optional.empty();
        }

        List<ElectricityReading> readings = electricityReadings.get();
        Map<String, BigDecimal> consumptionCosts = new HashMap<>();

        for (PricingStrategy strategy : strategyFactory.getAllStrategies()) {
            BigDecimal cost = strategy.calculateCost(readings);
            consumptionCosts.put(strategy.getPricePlanId(), cost);
        }

        Map<String, BigDecimal> sorted = consumptionCosts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return Optional.of(sorted);
    }

    public Optional<List<Map.Entry<String, BigDecimal>>> getRecommendedPlans(String smartMeterId, Integer limit) {

        Optional<List<ElectricityReading>> electricityReadings = meterReadingService.getReadings(smartMeterId);

        if (electricityReadings.isEmpty() || electricityReadings.get().isEmpty()) {
            return Optional.empty();
        }

        List<ElectricityReading> readings = electricityReadings.get();

        List<Map.Entry<String, BigDecimal>> recommendations = strategyFactory.getAllStrategies().stream()
                .map(strategy -> {
                    BigDecimal cost = strategy.calculateCost(readings);
                    return Map.entry(strategy.getPricePlanId(), cost);
                })
                .sorted(Map.Entry.comparingByValue())
                .limit(limit != null ? limit : Integer.MAX_VALUE)
                .collect(Collectors.toList());

        return Optional.of(recommendations);
    }
}
