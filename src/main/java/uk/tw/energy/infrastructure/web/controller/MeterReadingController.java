package uk.tw.energy.infrastructure.web.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.application.service.MeterReadingService;
import uk.tw.energy.domain.electricity.ElectricityReading;
import uk.tw.energy.domain.electricity.MeterReadings;

@Validated
@RestController
@RequestMapping("/readings")
@AllArgsConstructor
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping("/store")
    public ResponseEntity<Void> storeReadings(@Valid @RequestBody MeterReadings meterReadings) {
        List<ElectricityReading> electricityReadings = meterReadings.electricityReadings();
        meterReadingService.storeReadings(meterReadings.smartMeterId(), electricityReadings);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/read/{smartMeterId}")
    public ResponseEntity<List<ElectricityReading>> readReadings(@PathVariable String smartMeterId) {
        List<ElectricityReading> readings = meterReadingService.getReadings(smartMeterId);
        return ResponseEntity.ok(readings);
    }
}
