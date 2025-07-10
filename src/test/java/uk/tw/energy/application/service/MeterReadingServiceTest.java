package uk.tw.energy.application.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.tw.energy.domain.electricity.ElectricityReading;
import uk.tw.energy.infrastructure.web.exception.NotFoundException;

public class MeterReadingServiceTest {

    private MeterReadingService meterReadingService;
    public static final String UNKNOWN_METER_ID = "unknown-id";
    public static final String TEST_METER_ID = "test-meter-id";

    @BeforeEach
    public void setUp() {
        AccountService accountService = new AccountService(new HashMap<>());
        meterReadingService = new MeterReadingService(new HashMap<>(), accountService);
    }

    @Test
    public void givenMeterIdThatDoesNotExistShouldReturnNull() {
        assertThrows(NotFoundException.class, () -> {
            meterReadingService.getReadings(UNKNOWN_METER_ID);
        });
    }

    @Test
    public void givenMeterReadingThatExistsShouldReturnMeterReadings() {
        List<ElectricityReading> emptyReadings = new ArrayList<>();
        meterReadingService.storeReadings(TEST_METER_ID, emptyReadings);
        List<ElectricityReading> actualReadings = meterReadingService.getReadings(TEST_METER_ID);
        assertEquals(emptyReadings, actualReadings);
    }
}
