package uk.tw.energy;

import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.tw.energy.application.service.AccountService;

@TestConfiguration
public class TestAccountConfig {
    @Bean
    public AccountService accountService() {
        return new AccountService(Map.of(
                "bob", "price-plan-0",
                "alice", "price-plan-1",
                "jane", "price-plan-2"));
    }
}
