package uk.tw.energy.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.domain.ElectricityReading;
import uk.tw.energy.domain.MeterReadings;
import uk.tw.energy.exception.NotFoundException;
import uk.tw.energy.service.MeterReadingService;

@Validated
@RestController
@RequestMapping("/readings")
@AllArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping("/store")
    public ResponseEntity<Void> storeReadings(@Valid @RequestBody MeterReadings meterReadings) {
        String smartMeterId = meterReadings.smartMeterId();
        List<ElectricityReading> electricityReadings = meterReadings.electricityReadings();
        meterReadingService.storeReadings(smartMeterId, electricityReadings);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/read/{smartMeterId}")
    public ResponseEntity<List<ElectricityReading>> readReadings(@PathVariable String smartMeterId) {
        List<ElectricityReading> readings = getReadingsOrThrow(smartMeterId);
        return ResponseEntity.ok(readings);
    }

    private List<ElectricityReading> getReadingsOrThrow(String smartMeterId) {
        Optional<List<ElectricityReading>> readings = meterReadingService.getReadings(smartMeterId);
        if (readings.isEmpty()) {
            throw new NotFoundException("No readings found for smart meter ID: " + smartMeterId);
        }
        return readings.get();
    }
}
