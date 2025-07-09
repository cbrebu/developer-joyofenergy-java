package uk.tw.energy.application.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final Map<String, String> smartMeterToPricePlanAccounts;

    public AccountService(Map<String, String> smartMeterToPricePlanAccounts) {
        this.smartMeterToPricePlanAccounts = new HashMap<>(smartMeterToPricePlanAccounts);
    }

    public String getPricePlanIdForSmartMeterId(String smartMeterId) {
        String defaultPricePlanId = "price-plan-0";
        return smartMeterToPricePlanAccounts.getOrDefault(smartMeterId, defaultPricePlanId);
    }

    public void setPricePlanForMeter(String smartMeterId, String pricePlanId) {
        smartMeterToPricePlanAccounts.put(smartMeterId, pricePlanId);
    }

    public boolean hasMeter(String smartMeterId) {
        return smartMeterToPricePlanAccounts.containsKey(smartMeterId);
    }
}
