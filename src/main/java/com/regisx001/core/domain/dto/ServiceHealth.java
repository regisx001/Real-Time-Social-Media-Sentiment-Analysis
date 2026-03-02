package com.regisx001.core.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
