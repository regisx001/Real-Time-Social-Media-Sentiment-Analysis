package com.regisx001.core.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostgresMetrics(
        String status,
        Long latencyMs,
        String message,
        String version,
        int activeConnections,
        int maxConnections,
        long dbSizeBytes,
        String dbSizeHuman,
        long uptimeSeconds) {
}
