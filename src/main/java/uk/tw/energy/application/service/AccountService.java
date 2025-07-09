package uk.tw.energy.application.service;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final Map<String, String> smartMeterToPricePlanAccounts;

    public AccountService(Map<String, String> smartMeterToPricePlanAccounts) {
        this.smartMeterToPricePlanAccounts = smartMeterToPricePlanAccounts;
    }

    public String getPricePlanIdForSmartMeterId(String smartMeterId) {
        String defaultPricePlanId = "price-plan-0";
        return smartMeterToPricePlanAccounts.getOrDefault(smartMeterId, defaultPricePlanId);
    }
}
