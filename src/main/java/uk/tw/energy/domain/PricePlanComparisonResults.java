package uk.tw.energy.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PricePlanComparisonResults {
    private String pricePlanId;
    private Map<String, BigDecimal> pricePlanComparisons;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("pricePlanId", pricePlanId);
        map.put("pricePlanComparisons", pricePlanComparisons);
        return map;
    }
}
