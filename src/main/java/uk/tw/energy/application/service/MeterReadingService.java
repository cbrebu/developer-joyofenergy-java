package uk.tw.energy.application.service;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.tw.energy.domain.electricity.ElectricityReading;
import uk.tw.energy.infrastructure.web.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class MeterReadingService {

    @NotNull private final Map<String, List<ElectricityReading>> meterAssociatedReadings;

    @NotNull private final AccountService accountService;

    public static final String DEFAULT_PRICE_PLAN_ID = "price-plan-0";

    public List<ElectricityReading> getReadings(String smartMeterId) {
        Optional<List<ElectricityReading>> readings = Optional.ofNullable(meterAssociatedReadings.get(smartMeterId));
        if (readings.isEmpty()) {
            throw new NotFoundException("No readings found for smart meter ID: " + smartMeterId);
        }
        return readings.get();
    }

    public void storeReadings(String smartMeterId, List<ElectricityReading> electricityReadings) {
        if (!accountService.hasMeter(smartMeterId)) {
            accountService.setPricePlanForMeter(smartMeterId, DEFAULT_PRICE_PLAN_ID);
        }
        if (!meterAssociatedReadings.containsKey(smartMeterId)) {
            meterAssociatedReadings.put(smartMeterId, new ArrayList<>());
        }
        meterAssociatedReadings.get(smartMeterId).addAll(electricityReadings);
    }
}
