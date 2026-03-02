package com.regisx001.core.domain.dto;

import java.time.Instant;
import java.util.List;

public record HealthReport(
        String overall,
        Instant timestamp,
        List<ServiceHealth> services) {

    public static HealthReport of(List<ServiceHealth> services) {
        boolean allUp = services.stream().allMatch(s -> "UP".equals(s.status()));
        return new HealthReport(
                allUp ? "UP" : "DEGRADED",
                Instant.now(),
                services);
    }
}
