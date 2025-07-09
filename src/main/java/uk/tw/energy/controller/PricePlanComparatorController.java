package uk.tw.energy.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.tw.energy.domain.PricePlanComparisonResults;
import uk.tw.energy.exception.NotFoundException;
import uk.tw.energy.service.AccountService;
import uk.tw.energy.service.PricePlanService;

@RestController
@RequestMapping("/price-plans")
public class PricePlanComparatorController {

    private final PricePlanService pricePlanService;
    private final AccountService accountService;

    public PricePlanComparatorController(PricePlanService pricePlanService, AccountService accountService) {
        this.pricePlanService = pricePlanService;
        this.accountService = accountService;
    }

    @GetMapping("/compare-all/{smartMeterId}")
    public ResponseEntity<Map<String, Object>> comparePlans(@PathVariable String smartMeterId) {
        String pricePlanId = accountService.getPricePlanIdForSmartMeterId(smartMeterId);
        if (pricePlanId == null || pricePlanId.isEmpty()) {
            throw new NotFoundException("Price plan not found for smart meter ID: " + smartMeterId);
        }

        Optional<Map<String, BigDecimal>> consumptionsForPricePlans =
                pricePlanService.calculateConsumptionCostsForAllPricePlans(smartMeterId);

        if (consumptionsForPricePlans.isEmpty()) {
            throw new NotFoundException("No consumption data found for smart meter ID: " + smartMeterId);
        }

        PricePlanComparisonResults pricePlanComparator = PricePlanComparisonResults.builder()
                .pricePlanId(pricePlanId)
                .pricePlanComparisons(consumptionsForPricePlans.get())
                .build();

        return ResponseEntity.ok(pricePlanComparator.toMap());
    }

    @GetMapping("/recommend/{smartMeterId}")
    public ResponseEntity<List<Map.Entry<String, BigDecimal>>> recommendedPlans(
            @PathVariable String smartMeterId, @RequestParam(value = "limit", required = false) Integer limit) {

        Optional<List<Map.Entry<String, BigDecimal>>> recommendations =
                pricePlanService.getRecommendedPlans(smartMeterId, limit);

        if (recommendations.isEmpty()) {
            throw new NotFoundException("No consumption data found for smart meter ID: " + smartMeterId);
        }

        return ResponseEntity.ok(recommendations.get());
    }
}
