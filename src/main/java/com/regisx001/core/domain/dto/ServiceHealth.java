package com.regisx001.core.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the health status of a specific service.
 *
 * @param service   the name of the service
 * @param status    the health status (e.g., UP, DOWN)
 * @param message   an optional message or error reason
 * @param latencyMs the latency to the service in milliseconds, if applicable
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceHealth(
        String service,
        String status,
        String message,
        Long latencyMs) {

    public static ServiceHealth up(String service, long latencyMs) {
        return new ServiceHealth(service, "UP", null, latencyMs);
    }

    public static ServiceHealth down(String service, String reason) {
        return new ServiceHealth(service, "DOWN", reason, null);
    }
}
