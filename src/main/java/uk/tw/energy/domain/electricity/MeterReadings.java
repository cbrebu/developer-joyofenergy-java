package uk.tw.energy.domain.electricity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MeterReadings(
        @NotNull(message = "Smart Meter Id cannot be null") @NotBlank(message = "Smart Meter Id cannot be blank")
                String smartMeterId,
        @Valid
                @NotNull(message = "Electricity readings cannot be null") @NotEmpty(message = "Electricity readings cannot be empty")
                List<ElectricityReading> electricityReadings) {}
