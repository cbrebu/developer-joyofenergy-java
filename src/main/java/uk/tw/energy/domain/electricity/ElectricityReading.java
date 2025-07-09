package uk.tw.energy.domain.electricity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;

public record ElectricityReading(
        @NotNull(message = "Time cannot be null") Instant time,
        @PositiveOrZero(message = "Reading cannot be negative") @NotNull(message = "Reading cannot be null") @Max(10000)
                BigDecimal reading) {}
