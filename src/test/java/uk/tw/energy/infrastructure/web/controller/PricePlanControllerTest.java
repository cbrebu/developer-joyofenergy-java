package uk.tw.energy.infrastructure.web.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.tw.energy.application.service.AccountService;
import uk.tw.energy.application.service.MeterReadingService;
import uk.tw.energy.application.service.PricePlanService;
import uk.tw.energy.domain.electricity.ElectricityReading;
import uk.tw.energy.domain.pricing.MeterPricePlanRequest;
import uk.tw.energy.domain.pricing.strategy.PricingStrategy;
import uk.tw.energy.domain.pricing.strategy.PricingStrategyFactory;
import uk.tw.energy.domain.pricing.strategy.impl.DrEvilDarkEnergyStrategy;
import uk.tw.energy.domain.pricing.strategy.impl.DraculaEnergyStrategy;
import uk.tw.energy.domain.pricing.strategy.impl.PowerForEveryoneStrategy;
import uk.tw.energy.domain.pricing.strategy.impl.TheGreenEcoStrategy;
import uk.tw.energy.infrastructure.web.exception.NotFoundException;

public class PricePlanControllerTest {
    private static final String DRACULA_PLAN_ID = "price-plan-3";
    private static final String WORST_PLAN_ID = "price-plan-0";
    private static final String BEST_PLAN_ID = "price-plan-2";
    private static final String SECOND_BEST_PLAN_ID = "price-plan-1";
    private static final String SMART_METER_ID = "smart-meter-id";
    private static final String NON_EXISTENT_METER_ID = "non-existent-meter";

    private PricePlanController controller;
    private MeterReadingService meterReadingService;
    private AccountService accountService;

    @BeforeEach
    public void setUp() {
        List<PricingStrategy> pricingStrategies = List.of(
                new DrEvilDarkEnergyStrategy(),
                new TheGreenEcoStrategy(),
                new PowerForEveryoneStrategy(),
                new DraculaEnergyStrategy());

        meterReadingService = new MeterReadingService(new HashMap<>());
        PricingStrategyFactory pricingStrategyFactory = new PricingStrategyFactory(pricingStrategies);
        Map<String, String> smartMeterToPricePlanAccounts = new HashMap<>();
        smartMeterToPricePlanAccounts.put(SMART_METER_ID, WORST_PLAN_ID);
        accountService = new AccountService(smartMeterToPricePlanAccounts);
        PricePlanService pricePlanService =
                new PricePlanService(pricingStrategyFactory, meterReadingService, accountService);

        controller = new PricePlanController(pricePlanService, accountService);
    }

    @Test
    public void assignMeter_happyPath() {
        var request = new MeterPricePlanRequest(SMART_METER_ID, BEST_PLAN_ID);

        ResponseEntity<Void> response = controller.assignPricePlanToMeter(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
        assertThat(accountService.getPricePlanIdForSmartMeterId(SMART_METER_ID)).isEqualTo(BEST_PLAN_ID);
    }

    @Test
    public void assignMeter_nonExistentMeter() {
        var request = new MeterPricePlanRequest(NON_EXISTENT_METER_ID, BEST_PLAN_ID);

        assertThrows(IllegalArgumentException.class, () -> controller.assignPricePlanToMeter(request));
    }

    @Test
    public void assignMeter_nullSmartMeterId() {
        var request = new MeterPricePlanRequest(null, BEST_PLAN_ID);

        assertThrows(IllegalArgumentException.class, () -> controller.assignPricePlanToMeter(request));
    }

    @Test
    public void assignMeter_emptySmartMeterId() {
        var request = new MeterPricePlanRequest("", BEST_PLAN_ID);

        assertThrows(IllegalArgumentException.class, () -> controller.assignPricePlanToMeter(request));
    }

    @Test
    public void assignMeter_customPricePlan() {
        var customPricePlan = "custom-price-plan";
        var request = new MeterPricePlanRequest(SMART_METER_ID, customPricePlan);
        assertThrows(IllegalArgumentException.class, () -> controller.assignPricePlanToMeter(request));
    }

    @Test
    public void comparePlans_happyPath() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(15.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(5.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<Map<String, Object>> response = controller.comparePlans(SMART_METER_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> expected = Map.of(
                "pricePlanId",
                WORST_PLAN_ID,
                "pricePlanComparisons",
                Map.of(
                        WORST_PLAN_ID, BigDecimal.valueOf(100.0),
                        BEST_PLAN_ID, BigDecimal.valueOf(10.0),
                        SECOND_BEST_PLAN_ID, BigDecimal.valueOf(20.0),
                        DRACULA_PLAN_ID, BigDecimal.valueOf(500.0)));

        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    public void comparePlans_noReadings() {
        assertThrows(NotFoundException.class, () -> controller.comparePlans("not-found"));
    }

    @Test
    public void recommendedPlans_noLimit() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(1800), BigDecimal.valueOf(35.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response =
                controller.recommendedPlans(SMART_METER_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, BigDecimal.valueOf(38.0)),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, BigDecimal.valueOf(76.0)),
                new AbstractMap.SimpleEntry<>(WORST_PLAN_ID, BigDecimal.valueOf(380.0)),
                new AbstractMap.SimpleEntry<>(DRACULA_PLAN_ID, BigDecimal.valueOf(1900.0)));
        assertThat(response.getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    public void recommendedPlans_withLimit() {
        var electricityReading = new ElectricityReading(Instant.now().minusSeconds(2700), BigDecimal.valueOf(5.0));
        var otherReading = new ElectricityReading(Instant.now(), BigDecimal.valueOf(20.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(electricityReading, otherReading));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response = controller.recommendedPlans(SMART_METER_ID, 2);

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, BigDecimal.valueOf(16.7)),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, BigDecimal.valueOf(33.4)));
        assertThat(response.getBody()).isEqualTo(expectedPricePlanToCost);
    }

    @Test
    public void recommendedPlans_limitHigherThanNumberOfEntries() {
        var reading0 = new ElectricityReading(Instant.now().minusSeconds(3600), BigDecimal.valueOf(25.0));
        var reading1 = new ElectricityReading(Instant.now(), BigDecimal.valueOf(3.0));
        meterReadingService.storeReadings(SMART_METER_ID, List.of(reading0, reading1));

        ResponseEntity<List<Map.Entry<String, BigDecimal>>> response = controller.recommendedPlans(SMART_METER_ID, 5);

        var expectedPricePlanToCost = List.of(
                new AbstractMap.SimpleEntry<>(BEST_PLAN_ID, BigDecimal.valueOf(14.0)),
                new AbstractMap.SimpleEntry<>(SECOND_BEST_PLAN_ID, BigDecimal.valueOf(28.0)),
                new AbstractMap.SimpleEntry<>(WORST_PLAN_ID, BigDecimal.valueOf(140.0)),
                new AbstractMap.SimpleEntry<>(DRACULA_PLAN_ID, BigDecimal.valueOf(700.0)));
        assertThat(response.getBody()).isEqualTo(expectedPricePlanToCost);
    }
}
