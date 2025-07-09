package uk.tw.energy.domain.pricing;

import jakarta.validation.constraints.NotBlank;

public record MeterPricePlanRequest(
        @NotBlank(message = "Smart Meter ID cannot be null or empty") String smartMeterId,
        @NotBlank(message = "Price Plan ID cannot be null or empty") String pricePlanId) {}
