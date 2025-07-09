package uk.tw.energy.infrastructure.web.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RestApiError {

    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private List<String> details;

    public Map<String, Object> toMap() {
        return Map.of(
                "timestamp", timestamp,
                "status", status,
                "error", error,
                "message", message,
                "details", details);
    }
}
