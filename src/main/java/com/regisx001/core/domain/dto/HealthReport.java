package com.regisx001.core.domain.dto;

import java.time.Instant;
import java.util.List;

/**
 * An overall health report summarizing the status of multiple services.
 *
 * @param overall   the aggregated health status (e.g., UP, DEGRADED)
 * @param timestamp the exact time the health report was generated
 * @param services  a list of statuses for individual services
 */
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
