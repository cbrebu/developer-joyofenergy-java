package uk.tw.energy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import uk.tw.energy.builders.MeterReadingsBuilder;
import uk.tw.energy.domain.electricity.ElectricityReading;
import uk.tw.energy.domain.electricity.MeterReadings;
import uk.tw.energy.domain.pricing.MeterPricePlanRequest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {App.class, TestAccountConfig.class})
public class EndpointTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static HttpEntity<MeterReadings> toHttpEntity(MeterReadings meterReadings) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(meterReadings, headers);
    }

    @Test
    public void shouldStoreReadings() {
        MeterReadings meterReadings =
                new MeterReadingsBuilder().generateElectricityReadings().build();
        HttpEntity<MeterReadings> entity = toHttpEntity(meterReadings);

        ResponseEntity<String> response = restTemplate.postForEntity("/readings/store", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void givenMeterIdShouldReturnAMeterReadingAssociatedWithMeterId() {
        String smartMeterId = "alice";
        List<ElectricityReading> data = List.of(
                new ElectricityReading(Instant.parse("2024-04-26T00:00:10.00Z"), new BigDecimal(10)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:20.00Z"), new BigDecimal(20)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:30.00Z"), new BigDecimal(30)));
        populateReadingsForMeter(smartMeterId, data);

        ResponseEntity<ElectricityReading[]> response =
                restTemplate.getForEntity("/readings/read/" + smartMeterId, ElectricityReading[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Arrays.asList(response.getBody())).isEqualTo(data);
    }

    @Test
    public void shouldCalculateAllPrices() {
        String smartMeterId = "bob";
        List<ElectricityReading> data = List.of(
                new ElectricityReading(Instant.parse("2024-04-26T00:00:10.00Z"), new BigDecimal(10)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:20.00Z"), new BigDecimal(20)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:30.00Z"), new BigDecimal(30)));
        populateReadingsForMeter(smartMeterId, data);

        ResponseEntity<CompareAllResponse> response =
                restTemplate.getForEntity("/price-plans/compare-all/" + smartMeterId, CompareAllResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isEqualTo(new CompareAllResponse(
                        Map.of(
                                "price-plan-0",
                                36000,
                                "price-plan-1",
                                7200,
                                "price-plan-2",
                                3600,
                                "price-plan-3",
                                180000),
                        "price-plan-0"));
    }

    @Test
    public void givenMeterIdAndLimitShouldReturnRecommendedCheapestPricePlans() {
        String smartMeterId = "jane";
        List<ElectricityReading> data = List.of(
                new ElectricityReading(Instant.parse("2024-04-26T00:00:10.00Z"), new BigDecimal(10)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:20.00Z"), new BigDecimal(20)),
                new ElectricityReading(Instant.parse("2024-04-26T00:00:30.00Z"), new BigDecimal(30)));
        populateReadingsForMeter(smartMeterId, data);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/price-plans/recommend/" + smartMeterId + "?limit=2",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        List<Map<String, Object>> actualList = response.getBody();

        assertThat(actualList).hasSize(2);
        assertThat(actualList.get(0)).containsEntry("price-plan-2", 3600.0);
        assertThat(actualList.get(1)).containsEntry("price-plan-1", 7200.0);
    }

    @Test
    public void shouldAssignPricePlanToExistingMeter() {
        String existingMeterId = "smart-meter-0";
        String newPricePlan = "price-plan-2";

        MeterPricePlanRequest request = new MeterPricePlanRequest(existingMeterId, newPricePlan);
        HttpEntity<MeterPricePlanRequest> entity = toHttpEntity(request);

        ResponseEntity<Void> response = restTemplate.postForEntity("/price-plans/assign-meter", entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void shouldReturn400ForNonExistentMeter() {
        String nonExistentMeterId = "non-existent-meter";
        String pricePlan = "price-plan-1";

        MeterPricePlanRequest request = new MeterPricePlanRequest(nonExistentMeterId, pricePlan);
        HttpEntity<MeterPricePlanRequest> entity = toHttpEntity(request);

        ResponseEntity<Void> response = restTemplate.postForEntity("/price-plans/assign-meter", entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void shouldReturn400ForNonExistentPricePlan() {
        String existingMeterId = "smart-meter-0";
        String nonExistentPricePlan = "non-existent-price-plan";

        MeterPricePlanRequest request = new MeterPricePlanRequest(existingMeterId, nonExistentPricePlan);
        HttpEntity<MeterPricePlanRequest> entity = toHttpEntity(request);

        ResponseEntity<Void> response = restTemplate.postForEntity("/price-plans/assign-meter", entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void shouldReturn400ForEmptySmartMeterId() {
        MeterPricePlanRequest request = new MeterPricePlanRequest("", "price-plan-1");
        HttpEntity<MeterPricePlanRequest> entity = toHttpEntity(request);

        ResponseEntity<Void> response = restTemplate.postForEntity("/price-plans/assign-meter", entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void shouldReturn400ForEmptyPricePlanId() {
        MeterPricePlanRequest request = new MeterPricePlanRequest("smart-meter-0", "");
        HttpEntity<MeterPricePlanRequest> entity = toHttpEntity(request);

        ResponseEntity<Void> response = restTemplate.postForEntity("/price-plans/assign-meter", entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void shouldReturn400ForMalformedJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"invalid\": \"json\"}", headers);

        ResponseEntity<Void> response = restTemplate.postForEntity("/price-plans/assign-meter", entity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    private static HttpEntity<MeterPricePlanRequest> toHttpEntity(MeterPricePlanRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }

    private void populateReadingsForMeter(String smartMeterId, List<ElectricityReading> data) {
        MeterReadings readings = new MeterReadings(smartMeterId, data);
        HttpEntity<MeterReadings> entity = toHttpEntity(readings);
        restTemplate.postForEntity("/readings/store", entity, String.class);
    }

    record CompareAllResponse(Map<String, Integer> pricePlanComparisons, String pricePlanId) {}
}
