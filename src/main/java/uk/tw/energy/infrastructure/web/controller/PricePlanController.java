package uk.tw.energy.infrastructure.web.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.application.dto.PricePlanComparisonResults;
import uk.tw.energy.application.service.PricePlanService;
import uk.tw.energy.domain.pricing.MeterPricePlanRequest;

@AllArgsConstructor
@RestController
@RequestMapping("/price-plans")
@Validated
public class PricePlanController {

    private final PricePlanService pricePlanService;

    @PostMapping("/assign-meter")
    public ResponseEntity<Void> assignPricePlanToMeter(@Valid @RequestBody MeterPricePlanRequest request) {
        pricePlanService.assignPricePlanToMeter(request.smartMeterId(), request.pricePlanId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/compare-all/{smartMeterId}")
    public ResponseEntity<Map<String, Object>> comparePlans(@PathVariable String smartMeterId) {
        PricePlanComparisonResults comparison = pricePlanService.comparePlansForSmartMeter(smartMeterId);
        return ResponseEntity.ok(comparison.toMap());
    }

    @GetMapping("/recommend/{smartMeterId}")
    public ResponseEntity<List<Map.Entry<String, BigDecimal>>> recommendedPlans(
            @PathVariable String smartMeterId,
            @Positive(message = "Limit must be a positive number") @RequestParam(value = "limit", required = false)
                    Integer limit) {
        List<Map.Entry<String, BigDecimal>> recommendations = pricePlanService.getRecommendedPlans(smartMeterId, limit);

        return ResponseEntity.ok(recommendations);
    }
}
