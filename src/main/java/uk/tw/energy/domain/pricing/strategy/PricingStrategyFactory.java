package uk.tw.energy.domain.pricing.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.tw.energy.domain.electricity.ElectricityReading;

@Component
public class PricingStrategyFactory {

    private final Map<String, PricingStrategy> strategies;

    @Getter
    private final List<PricingStrategy> allStrategies;

    @Autowired
    public PricingStrategyFactory(List<PricingStrategy> pricingStrategies) {
        this.allStrategies = pricingStrategies;
        this.strategies = pricingStrategies.stream()
                .collect(Collectors.toMap(PricingStrategy::getPricePlanId, Function.identity()));
    }

    public Optional<PricingStrategy> getStrategy(String pricePlanId) {
        return Optional.ofNullable(strategies.get(pricePlanId));
    }

    public Optional<PricingStrategy> getCheapestStrategy(List<ElectricityReading> readings) {
        return allStrategies.stream().min(Comparator.comparing(s -> s.calculateCost(readings)));
    }

    public List<String> getAvailablePricePlanIds() {
        return allStrategies.stream().map(PricingStrategy::getPricePlanId).collect(Collectors.toList());
    }

    public boolean hasPricePlan(String pricePlanId) {
        return allStrategies.stream()
                .anyMatch(strategy -> strategy.getPricePlanId().equals(pricePlanId));
    }
}
